package com.auction.server.pattern;
import com.auction.server.model.Art;
import com.auction.server.model.Electronics;
import com.auction.server.model.Item;
import com.auction.server.model.Vehicle;

import java.math.BigDecimal;

public class ItemFactory {


    public static Item createItem(String category,
                                  long itemId,
                                  long sellerId,
                                  String itemName,
                                  String description,
                                  BigDecimal startingPrice,
                                  BigDecimal currentPrice,
                                  String imageUrl,
                                  double reservePrice,
                                  Object... additionalParams) {

        switch (category.toUpperCase()) {

            case "ELECTRONICS": {
                String brand              = (String) additionalParams[0];
                int warrantyMonths        = toInt(additionalParams[1]);
                int year_of_manufacture   = toInt(additionalParams[2]);
                return new Electronics(
                        itemId, sellerId, itemName, description,
                        "ELECTRONICS",
                        startingPrice, currentPrice, imageUrl, reservePrice,
                        brand, warrantyMonths, year_of_manufacture
                );
            }

            case "ART": {
                String artist = (String) additionalParams[0];
                String style  = (String) additionalParams[1];
                return new Art(
                        itemId, sellerId, itemName, description,
                        "ART",
                        startingPrice, currentPrice, imageUrl, reservePrice,
                        artist, style
                );
            }

            case "VEHICLE": {
                String fuel_type        = (String) additionalParams[0];
                String type_of_vehicle  = (String) additionalParams[1];
                String color            = (String) additionalParams[2];
                int year                = toInt(additionalParams[3]);
                return new Vehicle(
                        itemId, sellerId, itemName, description,
                        "VEHICLE",
                        startingPrice, currentPrice, imageUrl, reservePrice,
                        fuel_type, type_of_vehicle, color, year
                );
            }

            default:
                throw new IllegalArgumentException("Category is invalid: " + category);
        }
    }
    private static int toInt(Object value) {
        if (value instanceof Integer) return (Integer) value;
        return Integer.parseInt((String) value);
    }
}
