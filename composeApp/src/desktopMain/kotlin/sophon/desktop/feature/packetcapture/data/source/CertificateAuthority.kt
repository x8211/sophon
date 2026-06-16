package sophon.desktop.feature.packetcapture.data.source

import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.BasicConstraints
import org.bouncycastle.asn1.x509.Extension
import org.bouncycastle.asn1.x509.GeneralName
import org.bouncycastle.asn1.x509.GeneralNames
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPEMWriter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import sophon.desktop.core.CACHE_HOME
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Security
import java.security.cert.X509Certificate
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * CA 证书管理单例，负责根证书的持久化（{CACHE_HOME}/ca/）及按主机名动态签发叶子证书。
 * 首次启动时自动生成 10 年有效期的根证书；叶子证书签发后缓存至内存，避免重复生成。
 */
object CertificateAuthority {

    private val caDir = File(CACHE_HOME, "ca").also { it.mkdirs() }
    private val caCertFile = File(caDir, "ca.crt")
    private val caKeyFile = File(caDir, "ca.key")

    private var caCert: X509Certificate
    private var caPrivateKey: PrivateKey

    private val sslContextCache = ConcurrentHashMap<String, SslContext>()
    private val leafKeyPairGenerator = KeyPairGenerator.getInstance("RSA")

    init {
        Security.addProvider(BouncyCastleProvider())
        leafKeyPairGenerator.initialize(2048)
        val (cert, key) = if (caCertFile.exists() && caKeyFile.exists()) {
            loadExistingCA()
        } else {
            createAndSaveCA()
        }
        caCert = cert
        caPrivateKey = key
    }

    fun getCaCertFile(): File = caCertFile

    fun getSslContextFor(host: String): SslContext {
        return sslContextCache.getOrPut(host) { buildSslContextFor(host) }
    }

    private fun buildSslContextFor(host: String): SslContext {
        val leafKeyPair = leafKeyPairGenerator.generateKeyPair()
        val issuer = X500Name("CN=MicoToolbox CA, O=MicoToolbox, C=CN")
        val subject = X500Name("CN=$host")
        val notBefore = Date(System.currentTimeMillis() - 86400_000L)
        val notAfter = Date(System.currentTimeMillis() + 365L * 86400_000L)
        val serial = BigInteger.valueOf(System.currentTimeMillis())

        val certBuilder = JcaX509v3CertificateBuilder(
            issuer, serial, notBefore, notAfter, subject, leafKeyPair.public
        )
        certBuilder.addExtension(
            Extension.subjectAlternativeName, false,
            GeneralNames(GeneralName(GeneralName.dNSName, host))
        )

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .setProvider("BC")
            .build(caPrivateKey)
        val leafCert = JcaX509CertificateConverter()
            .setProvider("BC")
            .getCertificate(certBuilder.build(signer))

        return SslContextBuilder.forServer(leafKeyPair.private, leafCert, caCert).build()
    }

    private fun loadExistingCA(): Pair<X509Certificate, PrivateKey> {
        val cert = PEMParser(FileReader(caCertFile)).use { parser ->
            JcaX509CertificateConverter().setProvider("BC")
                .getCertificate(parser.readObject() as org.bouncycastle.cert.X509CertificateHolder)
        }
        val privateKey = PEMParser(FileReader(caKeyFile)).use { parser ->
            val pemKeyPair = parser.readObject() as PEMKeyPair
            JcaPEMKeyConverter().setProvider("BC").getKeyPair(pemKeyPair).private
        }
        return cert to privateKey
    }

    private fun createAndSaveCA(): Pair<X509Certificate, PrivateKey> {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val name = X500Name("CN=MicoToolbox CA, O=MicoToolbox, C=CN")
        val notBefore = Date(System.currentTimeMillis() - 86400_000L)
        val notAfter = Date(System.currentTimeMillis() + 10 * 365L * 86400_000L)
        val serial = BigInteger.valueOf(System.currentTimeMillis())

        val certBuilder = JcaX509v3CertificateBuilder(
            name, serial, notBefore, notAfter, name, keyPair.public
        )
        certBuilder.addExtension(Extension.basicConstraints, true, BasicConstraints(true))

        val signer = JcaContentSignerBuilder("SHA256WithRSAEncryption")
            .setProvider("BC")
            .build(keyPair.private)
        val cert = JcaX509CertificateConverter().setProvider("BC")
            .getCertificate(certBuilder.build(signer))

        JcaPEMWriter(FileWriter(caCertFile)).use { it.writeObject(cert) }
        JcaPEMWriter(FileWriter(caKeyFile)).use { it.writeObject(keyPair) }

        return cert to keyPair.private
    }
}
