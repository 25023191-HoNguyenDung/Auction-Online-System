package com.auction.server.pattern;
import com.auction.server.model.Product;
import com.auction.server.model.Electronics;
import com.auction.server.model.Art;
import com.auction.server.model.Vehicle;

public class ProductFactory {
    
    public static Product createProduct(String type, 
                                        String product_name, 
                                        String description, 
                                        double reversePrice,
                                        String image_url,
                                        Object... additionalParams) {

        switch ( type.toUpperCase() ) {

            case "ELECTRONICS":
                String brand = (String) additionalParams[0];
                int warrantyMonths = Integer.parseInt((String) additionalParams[1]);
                int year_of_manufacture = Integer.parseInt((String) additionalParams[2]);
                return new Electronics( product_name,
                                        description,
                                        reversePrice,
                                        image_url,
                                        brand,
                                        warrantyMonths,
                                        year_of_manufacture);

            case "ART":
                String artist = (String) additionalParams[0];
                String style = (String) additionalParams[1];
                return new Art( product_name,
                                description,
                                reversePrice,
                                image_url,
                                artist,
                                style);

            case "VEHICLE":
                String fuel_type = (String) additionalParams[0];
                String type_of_vehicle = (String) additionalParams[1];
                String color = (String) additionalParams[2];
                int year = Integer.parseInt((String) additionalParams[3]);
                return new Vehicle( product_name,
                                    description,
                                    reversePrice,
                                    image_url,
                                    fuel_type,
                                    type_of_vehicle,
                                    color,
                                    year);
            default:
                throw new IllegalArgumentException("Invalid product type: " + type);
        }
    }
}
