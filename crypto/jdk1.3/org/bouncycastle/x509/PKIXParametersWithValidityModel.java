package org.bouncycastle.x509;

import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import org.bouncycastle.jce.cert.PKIXParameters;
import org.bouncycastle.jce.cert.TrustAnchor;
import java.util.Set;

/**
 * This class extends the PKIXParameters with a validity model parameter.
 */
public class PKIXParametersWithValidityModel
    extends PKIXParameters
{

    /**
     * This is the default PKIX validity model. Actually there are two variants
     * of this: The PKIX model and the modified PKIX model. The PKIX model
     * verifies that all involved certificates must have been valid at the
     * current time. The modified PKIX model verifies that all involved
     * certificates were valid at the signing time. Both are indirectly choosen
     * with the {@link PKIXParameters#setDate(java.util.Date)} method, so this
     * methods sets the Date when <em>all</em> certificates must have been valid.
     */
    public static final int PKIX_VALIDITY_MODEL = 0;

    /**
     * This model uses the following validity model. Each certificate must have
     * been valid at the moment where is was used. That means teh end
     * certificate must have been valid at the time the signature was done. The
     * CA certificate which signed the end certificate must have been valid,
     * when the end certificate was signed. The CA (or Root CA) certificate must
     * have been valid, when the CA certificate was signed and so on. So the
     * {@link PKIXParameters#setDate(java.util.Date)} method sets the time, when
     * the <em>end certificate</em> must have been valid.
     * <p/>
     * It is used e.g. in the German signature law.
     */
    public static final int CHAIN_VALIDITY_MODEL = 1;

    private int validityModel = PKIX_VALIDITY_MODEL;

    /**
     * Creates an instance of <code>PKIXParameters</code> with the specified
     * <code>Set</code> of most-trusted CAs. Each element of the set is a
     * {@link TrustAnchor TrustAnchor}.
     * <p/>
     * Note that the <code>Set</code> is copied to protect against subsequent
     * modifications.
     *
     * @param trustAnchors a <code>Set</code> of <code>TrustAnchor</code>s
     * @throws InvalidAlgorithmParameterException
     *                              if the specified <code>Set</code> is empty
     *                              <code>(trustAnchors.isEmpty() == true)</code>
     * @throws NullPointerException if the specified <code>Set</code> is <code>null</code>
     * @throws ClassCastException   if any of the elements in the <code>Set</code> are not of
     *                              type <code>java.security.cert.TrustAnchor</code>
     */
    public PKIXParametersWithValidityModel(Set trustAnchors)
        throws InvalidAlgorithmParameterException
    {
        super(trustAnchors);
    }

    /**
     * Creates an instance of <code>PKIXParameters</code> that populates the
     * set of most-trusted CAs from the trusted certificate entries contained in
     * the specified <code>KeyStore</code>. Only keystore entries that
     * contain trusted <code>X509Certificates</code> are considered; all other
     * certificate types are ignored.
     *
     * @param keystore a <code>KeyStore</code> from which the set of most-trusted
     *                 CAs will be populated
     * @throws KeyStoreException    if the keystore has not been initialized
     * @throws InvalidAlgorithmParameterException
     *                              if the keystore does not contain at least one trusted
     *                              certificate entry
     * @throws NullPointerException if the keystore is <code>null</code>
     */
    public PKIXParametersWithValidityModel(KeyStore keystore)
        throws KeyStoreException, InvalidAlgorithmParameterException
    {
        super(keystore);
    }

    /**
     * @return Returns the validity model.
     * @see #CHAIN_VALIDITY_MODEL
     * @see #PKIX_VALIDITY_MODEL
     */
    public int getValidityModel()
    {
        return validityModel;
    }

    /**
     * @param validityModel The validity model to set.
     * @see #CHAIN_VALIDITY_MODEL
     * @see #PKIX_VALIDITY_MODEL
     */
	public void setValidityModel(int validityModel)
    {
		this.validityModel = validityModel;
	}
}
