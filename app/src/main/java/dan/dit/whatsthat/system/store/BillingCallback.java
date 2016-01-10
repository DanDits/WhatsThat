package dan.dit.whatsthat.system.store;

/**
 * Created by daniel on 09.01.16.
 */
public interface BillingCallback {
    boolean isAvailable();
    void purchase(String productId, StoreActivity.ProductPurchasedCallback callback);
}
