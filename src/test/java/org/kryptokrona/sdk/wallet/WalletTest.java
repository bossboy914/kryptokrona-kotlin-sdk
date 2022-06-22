package org.kryptokrona.sdk.wallet;

import inet.ipaddr.HostName;
import org.junit.jupiter.api.*;
import org.kryptokrona.sdk.daemon.Daemon;
import org.kryptokrona.sdk.daemon.DaemonBasic;
import org.kryptokrona.sdk.exception.NetworkBlockCountException;
import org.kryptokrona.sdk.service.WalletService;

import java.io.IOException;

class WalletTest {

    private Daemon daemon;
    private WalletService walletService;

    /*@BeforeEach
    void beforeEach() {
        daemon = new DaemonBasic(new HostName("gota.kryptokrona.se:11898"));
        walletService = new WalletService();
    }

    @Test
    @DisplayName("Create Wallet")
    void createWalletTest() throws IOException, NetworkBlockCountException {
        daemon.init().subscribe(System.out::println);
        walletService.createWallet(daemon);
    }

    @Test
    @DisplayName("Start/Stop Wallet")
    void startStopWalletTest() throws IOException, NetworkBlockCountException {
        daemon.init().subscribe(System.out::println);
        Wallet wallet = walletService.createWallet(daemon);
        wallet.start();
        wallet.stop();
    }

    @Test
    @DisplayName("Save Wallet To File")
    void saveWalletToFileTest() throws IOException, NetworkBlockCountException {
        daemon.init().subscribe(System.out::println);
        Wallet wallet = walletService.createWallet(daemon);
        wallet.saveToFile("test.wallet", "test1234");
    }*/
}