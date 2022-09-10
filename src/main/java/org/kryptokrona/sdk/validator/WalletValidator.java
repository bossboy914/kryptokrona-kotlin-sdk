package org.kryptokrona.sdk.validator;

import io.reactivex.rxjava3.core.Observable;
import org.kryptokrona.sdk.config.Config;
import org.kryptokrona.sdk.exception.wallet.*;
import org.kryptokrona.sdk.model.util.FeeType;
import org.kryptokrona.sdk.wallet.Address;
import org.kryptokrona.sdk.wallet.SubWallets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * WalletValidator.java
 *
 * @author Marcus Cvjeticanin (@mjovanc)
 */
public class WalletValidator {

	/**
	 * Verifies that the address given is valid.
	 *
	 * @param address The address to validate.
	 * @param integratedAddressAllowed Should an integrated address be allowed?
	 * @return Returns true if the address is valid, otherwise throws exception descripting the error
	 */
	public Observable<Boolean> validateAddress(String address, boolean integratedAddressAllowed) throws WalletException {
		return validateAddresses(List.of(address), integratedAddressAllowed);
	}

	/**
	 * Verifies that the addresses given are valid.
	 *
	 * @param addresses The addresses to validate
	 * @param integratedAddressesAllowed Should we allow integrated addresses?
	 * @return Returns an Observable of type boolean if address is valid, otherwise throws exception describing the error
	 */
	public Observable<Boolean> validateAddresses(List<String> addresses, boolean integratedAddressesAllowed)
			throws WalletException {

		var alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";

		for (String address : addresses) {
			// verify that address lengths are correct
			if (
					address.length() != Config.STANDARD_ADDRESS_LENGTH &&
					address.length() != Config.INTEGRATED_ADDRESS_LENGTH) {
				throw new WalletAddressWrongLengthException();
			}

			// verify every address character is in the base58 set
			char[] chars = address.toCharArray();
			for (char c : chars) {
				if (!contains(c, alphabet.toCharArray())) {
					throw new WalletAddressNotBase58Exception();
				}
			}

			// verify it's not an integrated, if those aren't allowed
			Address.fromAddress(address, Config.ADDRESS_PREFIX).subscribe(a -> {
				if (a.getPaymentId().length() != 0 && !integratedAddressesAllowed) {
					throw new WalletAddressIsIntegratedException();
				}
			});
		}

		return Observable.just(true);
	}

	/**
	 * Validate the amounts being sent are valid, and the addresses are valid.
	 *
	 * @return Returns true if valid, otherwise throws an exception describing the error
	 */
	public Observable<Boolean> validateDestinations(Map<String, Double> destinations) throws WalletException {
		if (destinations.size() == 0) {
			throw new WalletNoDestinationGivenException();
		}

		List<String> destinationAddresses = new ArrayList<>();

		for (Map.Entry<String, Double> entry : destinations.entrySet()) {
			if (entry.getValue().intValue() == 0) {
				throw new WalletAmountIsZeroException();
			}

			if (entry.getValue().intValue() < 0) {
				throw new WalletNegativeValueGivenException();
			}

			destinationAddresses.add(entry.getKey());
		}

		return validateAddresses(destinationAddresses, true);
	}

	/**
	 * Validate that the payment ID's included in integrated addresses are valid.
	 * You should have already called validateAddresses() before this function.
	 *
	 * @return Returns true if valid, otherwise throws an exception describing the error
	 */
	public Observable<Boolean> validateIntegratedAddresses(List<Map<String, Number>> destinations, final String paymentID) throws WalletAddressChecksumMismatchException {
		for (var destination : destinations) {
			if (destination.keySet().size() != Config.INTEGRATED_ADDRESS_LENGTH) {
				continue;
			}

			/* Extract the payment ID */
			Address.fromAddress(destination.keySet().toString(), Config.ADDRESS_PREFIX)
					.subscribe(parsedAddress -> {
						if (paymentID != parsedAddress.getPaymentId()) {
							throw new WalletConflictingPaymentIdsException();
						}
					});
		}

		return Observable.just(true);
	}

	/**
	 * Validate the addresses given are both valid, and exist in the sub wallet.
	 *
	 * @param addresses List of addresses to validate
	 * @param subWallets List of sub wallets to use in validation
	 * @return Returns SUCCESS if valid, otherwise a WalletError describing the error
	 */
	public Observable<Boolean> validateOurAddresses(List<String> addresses, SubWallets subWallets) throws WalletException {
		validateAddresses(addresses, false)
				.subscribe(validity -> {
					for (var address : addresses) {
						Address.fromAddress(address, Config.ADDRESS_PREFIX)
								.subscribe(parsedAddress -> {
									var keys = subWallets.getPublicSpendKeys();

									//TODO: below is probably not finished yet
									if (!keys.contains(parsedAddress.getSpendKeys())) {
										throw new WalletAddressNotInWalletException();
									}
								});
					}
				});

		return Observable.just(true);
	}

	/**
	 * Validate that the transfer amount + fee is valid, and we have enough balance
	 * Note: Does not verify amounts are positive / integer, validateDestinations
	 * handles that.
	 *
	 * @return Returns true if valid, otherwise an exception describing the error
	 */
	public Observable<Boolean> validateAmount(
			Map<String, Number> destinations,
			FeeType feeType,
			List<String> subWalletsToTakeFrom,
			SubWallets subWallets,
			long currentHeight) throws WalletFeeTooSmallException {

		if (!feeType.isFeePerByte() && !feeType.isFixedFee()) {
			throw new WalletFeeTooSmallException();
		}

		subWallets.getBalance(currentHeight, subWalletsToTakeFrom)
				.subscribe(summedBalance -> {
					var totalAmount = 0.0; //TODO: fix this.

					if (feeType.isFixedFee()) {
						totalAmount += feeType.getFixedFee();
					}

					for (Double amount : summedBalance.keySet()) {
						if (totalAmount > amount) {
							throw new WalletNotEnoughBalanceException();
						}
					}

					if (totalAmount >= 2 * Math.pow(2, 64)) {
						throw new WalletWillOverflowException();
					}
				});

		return Observable.just(true);
	}

	/**
	 * Validates mixin is valid and in allowed range
	 *
	 * @param mixin The mixin to validate
	 * @param height Height for getting the mixin
	 * @return Returns true if valid, otherwise throws an exception describing the error
	 */
	public Observable<Boolean> validateMixin(long mixin, long height)
			throws WalletNegativeValueGivenException, WalletMixinTooSmallException, WalletMixinTooBigException {
		if (mixin < 0) {
			throw new WalletNegativeValueGivenException();
		}

		Map<String, Long> mixinLimitsByHeight = Config.MIXIN_LIMITS.getMixinLimitsByHeight(height);

		if (mixin < mixinLimitsByHeight.get("minMixin")) {
			throw new WalletMixinTooSmallException();
		}

		if (mixin > mixinLimitsByHeight.get("maxMixin")) {
			throw new WalletMixinTooBigException();
		}

		return Observable.just(true);
	}

	/**
	 * Validates the payment ID is valid (or an empty string)
	 *
	 * @param paymentID The payment ID to validate
	 * @param allowEmptyString If we want to allow an empty string
	 * @return Returns true if valid, otherwise throws an exception describing the error
	 */
	public Observable<Boolean> validatePaymentID(String paymentID, boolean allowEmptyString)
			throws WalletPaymentIdWrongLengthException, WalletPaymentIdInvalidException {

		if (Objects.equals(paymentID, "") && allowEmptyString) {
			return Observable.just(true);
		}

		if (paymentID.length() != 64) {
			throw new WalletPaymentIdWrongLengthException();
		}

		if (!paymentID.matches("^([a-zA-Z0-9]{64})?$")) {
			throw new WalletPaymentIdInvalidException();
		}

		return Observable.just(true);
	}

	/**
	 * Check if character exists in char array
	 *
	 * @param c The character to check if it contains in char array
	 * @param array The char array to check against
	 * @return Returns true if it contains, otherwise false
	 */
	private boolean contains(char c, char[] array) {
		for (char x : array) {
			if (x == c) {
				return true;
			}
		}
		return false;
	}
}
