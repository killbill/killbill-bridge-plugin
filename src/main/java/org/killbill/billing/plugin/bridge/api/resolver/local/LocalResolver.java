package org.killbill.billing.plugin.bridge.api.resolver.local;

import com.google.common.collect.ImmutableList;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.payment.api.Payment;
import org.killbill.billing.payment.api.PaymentApiException;
import org.killbill.billing.payment.api.PaymentMethod;
import org.killbill.billing.payment.plugin.api.PaymentPluginApiException;
import org.killbill.billing.util.callcontext.TenantContext;

import java.util.UUID;

public class LocalResolver {

    private final TenantContext tenantContext;
    private final OSGIKillbillAPI killbillAPI;

    public LocalResolver(final OSGIKillbillAPI killbillAPI, final TenantContext tenantContext) {
        this.tenantContext = tenantContext;
        this.killbillAPI = killbillAPI;
    }

    public String getPaymentMethodExternalKey(final UUID kbPaymentMethodId) throws PaymentPluginApiException {
        try {
            final PaymentMethod paymentMethod = killbillAPI.getPaymentApi().getPaymentMethodById(kbPaymentMethodId, false, false, ImmutableList.of(), tenantContext);
            return paymentMethod.getExternalKey();
        } catch (final PaymentApiException e) {
            throw new PaymentPluginApiException(String.format("Failed to retrieve payment method %s", kbPaymentMethodId), e);
        }
    }


    public String getAccountExternalKey(final UUID kbAccountId) throws PaymentPluginApiException {
        try {
            final Account account = killbillAPI.getAccountUserApi().getAccountById(kbAccountId, tenantContext);
            return account.getExternalKey();
        } catch (final AccountApiException e) {
            throw new PaymentPluginApiException(String.format("Failed to retrieve account %s", kbAccountId), e);
        }
    }

    public Payment getPayment(final UUID kbPaymentId) throws PaymentPluginApiException {
        try {
            return killbillAPI.getPaymentApi().getPayment(kbPaymentId, false, false, ImmutableList.of(), tenantContext);
        } catch (PaymentApiException e) {
            throw new PaymentPluginApiException(String.format("Failed to retrieve payment %s", kbPaymentId), e);
        }
    }


    public String getPaymentExternalKey(final UUID kbPaymentId) throws PaymentPluginApiException {
        final Payment payment = getPayment(kbPaymentId);
        return payment.getExternalKey();
    }
}



