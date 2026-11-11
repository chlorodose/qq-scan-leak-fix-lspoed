import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import java.io.*;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.*;

public final class Sign {
    public static void main(String[] args) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream in = new FileInputStream(args[0])) { ks.load(in, "local-build".toCharArray()); }
        PrivateKey key = (PrivateKey)ks.getKey("qqscanfix", "local-build".toCharArray());
        X509Certificate cert = (X509Certificate)ks.getCertificate("qqscanfix");
        ApkSigner.SignerConfig signer = new ApkSigner.SignerConfig.Builder(
                "qqscanfix", key, Collections.singletonList(cert)).build();
        new ApkSigner.Builder(Collections.singletonList(signer))
                .setInputApk(new File(args[1])).setOutputApk(new File(args[2]))
                .setMinSdkVersion(27).setV1SigningEnabled(true).setV2SigningEnabled(true)
                .setV3SigningEnabled(true).setV4SigningEnabled(false).build().sign();
        ApkVerifier.Result result = new ApkVerifier.Builder(new File(args[2])).build().verify();
        if (!result.isVerified()) throw new IllegalStateException(result.getErrors().toString());
        System.out.println("APK verified: v2="+result.isVerifiedUsingV2Scheme()+", v3="+result.isVerifiedUsingV3Scheme());
    }
}
