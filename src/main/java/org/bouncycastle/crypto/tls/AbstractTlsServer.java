package org.bouncycastle.crypto.tls;

import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

public abstract class AbstractTlsServer implements TlsServer {

    protected TlsCipherFactory cipherFactory;

    protected TlsServerContext context;

    protected ProtocolVersion clientVersion;
    protected int[] offeredCipherSuites;
    protected short[] offeredCompressionMethods;
    protected Hashtable clientExtensions;

    protected boolean eccCipherSuitesOffered;
    protected int[] namedCurves;
    protected short[] ecPointFormats;

    protected ProtocolVersion serverVersion;
    protected int selectedCipherSuite;
    protected short selectedCompressionMethod;
    protected Hashtable serverExtensions;

    public AbstractTlsServer() {
        this(new DefaultTlsCipherFactory());
    }

    public AbstractTlsServer(TlsCipherFactory cipherFactory) {
        this.cipherFactory = cipherFactory;
    }

    protected abstract int[] getCipherSuites();

    protected short[] getCompressionMethods() {
        return new short[] { CompressionMethod.NULL };
    }

    protected ProtocolVersion getMaximumVersion() {
        return ProtocolVersion.TLSv11;
    }

    protected ProtocolVersion getMinimumVersion() {
        return ProtocolVersion.TLSv10;
    }

    protected boolean supportsClientECCCapabilities(int[] namedCurves, short[] ecPointFormats) {

        // NOTE: BC supports all the current set of point formats so we don't check them here

        if (namedCurves == null) {
            /*
             * RFC 4492 4. A client that proposes ECC cipher suites may choose not to include these
             * extensions. In this case, the server is free to choose any one of the elliptic curves
             * or point formats [...].
             */
            return TlsECCUtils.hasAnySupportedNamedCurves();
        }

        for (int i = 0; i < namedCurves.length; ++i) {
            if (TlsECCUtils.isSupportedNamedCurve(namedCurves[i])) {
                return true;
            }
        }

        return false;
    }

    public void init(TlsServerContext context) {
        this.context = context;
    }

    public void notifyClientVersion(ProtocolVersion clientVersion) throws IOException {
        this.clientVersion = clientVersion;
    }

    public void notifyOfferedCipherSuites(int[] offeredCipherSuites) throws IOException {
        this.offeredCipherSuites = offeredCipherSuites;
        this.eccCipherSuitesOffered = TlsECCUtils.containsECCCipherSuites(this.offeredCipherSuites);
    }

    public void notifyOfferedCompressionMethods(short[] offeredCompressionMethods)
        throws IOException {
        this.offeredCompressionMethods = offeredCompressionMethods;
    }

    public void notifySecureRenegotiation(boolean secureRenegotiation) throws IOException {
        if (!secureRenegotiation) {
            /*
             * RFC 5746 3.6. In this case, some servers may want to terminate the handshake instead
             * of continuing; see Section 4.3 for discussion.
             */
            throw new TlsFatalAlert(AlertDescription.handshake_failure);
        }
    }

    public void processClientExtensions(Hashtable clientExtensions) throws IOException {

        this.clientExtensions = clientExtensions;

        if (clientExtensions != null) {
            this.namedCurves = TlsECCUtils.getSupportedEllipticCurvesExtension(clientExtensions);
            this.ecPointFormats = TlsECCUtils.getSupportedPointFormatsExtension(clientExtensions);
        }

        /*
         * RFC 4429 4. The client MUST NOT include these extensions in the ClientHello message if it
         * does not propose any ECC cipher suites.
         */
        if (!this.eccCipherSuitesOffered
            && (this.namedCurves != null || this.ecPointFormats != null)) {
            throw new TlsFatalAlert(AlertDescription.illegal_parameter);
        }
    }

    public ProtocolVersion getServerVersion() throws IOException {
        if (getMinimumVersion().isEqualOrEarlierVersionOf(clientVersion)) {
            ProtocolVersion maximumVersion = getMaximumVersion();
            if (clientVersion.isEqualOrEarlierVersionOf(maximumVersion)) {
                return serverVersion = clientVersion;
            }
            if (clientVersion.isLaterVersionOf(maximumVersion)) {
                return serverVersion = maximumVersion;
            }
        }
        throw new TlsFatalAlert(AlertDescription.protocol_version);
    }

    public int getSelectedCipherSuite() throws IOException {

        /*
         * RFC 4429 5.1. A server that receives a ClientHello containing one or both of these
         * extensions MUST use the client's enumerated capabilities to guide its selection of an
         * appropriate cipher suite. One of the proposed ECC cipher suites must be negotiated only
         * if the server can successfully complete the handshake while using the curves and point
         * formats supported by the client [...].
         */
        boolean eccCipherSuitesEnabled = supportsClientECCCapabilities(this.namedCurves,
            this.ecPointFormats);

        int[] cipherSuites = getCipherSuites();
        for (int i = 0; i < cipherSuites.length; ++i) {
            int cipherSuite = cipherSuites[i];
            if (TlsProtocol.arrayContains(this.offeredCipherSuites, cipherSuite)
                && (eccCipherSuitesEnabled || !TlsECCUtils.isECCCipherSuite(cipherSuite))) {
                return this.selectedCipherSuite = cipherSuite;
            }
        }
        throw new TlsFatalAlert(AlertDescription.handshake_failure);
    }

    public short getSelectedCompressionMethod() throws IOException {
        short[] compressionMethods = getCompressionMethods();
        for (int i = 0; i < compressionMethods.length; ++i) {
            if (TlsProtocol.arrayContains(offeredCompressionMethods, compressionMethods[i])) {
                return this.selectedCompressionMethod = compressionMethods[i];
            }
        }
        throw new TlsFatalAlert(AlertDescription.handshake_failure);
    }

    // Hashtable is (Integer -> byte[])
    public Hashtable getServerExtensions() throws IOException {

        if (this.ecPointFormats != null && TlsECCUtils.isECCCipherSuite(this.selectedCipherSuite)) {
            /*
             * RFC 4492 5.2. A server that selects an ECC cipher suite in response to a ClientHello
             * message including a Supported Point Formats Extension appends this extension (along
             * with others) to its ServerHello message, enumerating the point formats it can parse.
             */
            this.serverExtensions = new Hashtable();
            TlsECCUtils.addSupportedPointFormatsExtension(serverExtensions, new short[] {
                ECPointFormat.ansiX962_compressed_char2, ECPointFormat.ansiX962_compressed_prime,
                ECPointFormat.uncompressed });
            return serverExtensions;
        }

        return null;
    }

    public Vector getServerSupplementalData() throws IOException {
        return null;
    }

    public CertificateRequest getCertificateRequest() {
        return null;
    }

    public void processClientSupplementalData(Vector clientSupplementalData) throws IOException {
        if (clientSupplementalData != null) {
            throw new TlsFatalAlert(AlertDescription.unexpected_message);
        }
    }

    public TlsCompression getCompression() throws IOException {
        switch (selectedCompressionMethod) {
        case CompressionMethod.NULL:
            return new TlsNullCompression();

        default:
            /*
             * Note: internal error here; we selected the compression method, so if we now can't
             * produce an implementation, we shouldn't have chosen it!
             */
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    public void notifyHandshakeComplete() throws IOException {
    }
}