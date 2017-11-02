package com.klein.btc;

import java.util.HashMap;
import java.util.Map;

public enum  Product {
    BTC(ProductType.CRYPTO), EUR(ProductType.FIAT), USD(ProductType.FIAT), BTCEUR(BTC,EUR), BTCUSD(BTC,USD);

    private static Map<String, Product> gdaxLookupTable=new HashMap<>();
    static {
        for (Product product : Product.values()) {
            if (product.type==ProductType.EXCHANGE_PAIR){
                gdaxLookupTable.put(product.getGdaxCode(), product);
            }
        }
    }

    private Product p1;
    private Product p2;
    private ProductType type;

    Product(ProductType type) {
        this.type = type;
    }
    Product(Product p1, Product p2) {
        this.type=ProductType.EXCHANGE_PAIR;
        this.p1 = p1;
        this.p2 = p2;
    }


    public String getGdaxCode() {
        return p1.name() + "-" + p2.name();
    }

    public String getBitfinexCode() {
        return p1.name() + p2.name();
    }

    public static Product getProductForGdaxCode(String code){
        return gdaxLookupTable.get(code);
    }
}
