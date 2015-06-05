package org.multibit.viewsystem.swing.action;

import java.io.File;
import java.security.SecureRandom;

import org.bitcoinj.wallet.Protos;
import org.bitcoinj.wallet.Protos.ScryptParameters;
import org.multibit.controller.bitcoin.BitcoinController;
import org.multibit.file.FileHandler;
import org.multibit.model.bitcoin.WalletAddressBookData;
import org.multibit.model.bitcoin.WalletData;
import org.multibit.model.bitcoin.WalletInfoData;
import org.multibit.store.MultiBitWalletVersion;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import com.google.protobuf.ByteString;
import org.bitcoinj.wallet.KeyChainGroup;

/**
 * Class containing utility methods for action tests.
 * @author jim
 *
 */
public class ActionTestUtils {
    
     private static SecureRandom secureRandom;

     public static final String LABEL_OF_ADDRESS_ADDED = "This is an address label";

     public static void createNewActiveWallet(BitcoinController controller, String descriptor, boolean encrypt, CharSequence walletPassword) throws Exception {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }

        byte[] salt = new byte[KeyCrypterScrypt.SALT_LENGTH];
        secureRandom.nextBytes(salt);
        Protos.ScryptParameters.Builder scryptParametersBuilder = Protos.ScryptParameters.newBuilder().setSalt(ByteString.copyFrom(salt));
        ScryptParameters scryptParameters = scryptParametersBuilder.build();
        NetworkParameters params = org.altcoinj.params.DogecoinMainNetParams.get();
        KeyCrypter keyCrypter = new KeyCrypterScrypt(scryptParameters);
        KeyChainGroup keyChainGroup = new KeyChainGroup(params);
        keyChainGroup.encrypt(keyCrypter, keyCrypter.deriveKey(walletPassword));
 
         Wallet wallet;
         ECKey ecKey;
         if (encrypt) {
             wallet = new Wallet(params, keyChainGroup);
             ecKey = (new ECKey()).encrypt(keyCrypter, keyCrypter.deriveKey(walletPassword));
             wallet.importKey(ecKey);
         } else {
             wallet = new Wallet(org.altcoinj.params.DogecoinMainNetParams.get());
             ecKey = new ECKey();
             wallet.importKey(ecKey);             
         }
         
         WalletData perWalletModelData = new WalletData();
         perWalletModelData.setWallet(wallet);
  
         // Save the wallet to a temporary directory.
         File multiBitDirectory = FileHandler.createTempDirectory("CreateAndDeleteWalletsTest");
         String multiBitDirectoryPath = multiBitDirectory.getAbsolutePath();
         String walletFile = multiBitDirectoryPath + File.separator + descriptor + ".wallet";
         
         // Put the wallet in the model as the active wallet.
         WalletInfoData walletInfoData = new WalletInfoData(walletFile, wallet, MultiBitWalletVersion.PROTOBUF_ENCRYPTED);
         walletInfoData.addReceivingAddress(new WalletAddressBookData(LABEL_OF_ADDRESS_ADDED, ecKey.toAddress(org.altcoinj.params.DogecoinMainNetParams.get()).toString()), false);

         perWalletModelData.setWalletInfo(walletInfoData);
         perWalletModelData.setWalletFilename(walletFile);
         perWalletModelData.setWalletDescription(descriptor);
         
         // Save the wallet and load it up again, making it the active wallet.
         // This also sets the timestamp fields used in file change detection.
         FileHandler fileHandler = new FileHandler(controller);
         fileHandler.savePerWalletModelData(perWalletModelData, true);
         WalletData loadedPerWalletModelData = fileHandler.loadFromFile(new File(walletFile));
         
         controller.getModel().setActiveWalletByFilename(loadedPerWalletModelData.getWalletFilename());         
     }
}
