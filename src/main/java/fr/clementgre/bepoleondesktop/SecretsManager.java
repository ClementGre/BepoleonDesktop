package fr.clementgre.bepoleondesktop;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SecretsManager {


    public static Properties loadSecrets(String fileName) {
        Properties properties = new Properties();

        try (InputStream input = Main.class.getClassLoader().getResourceAsStream(fileName)) {
            if (input == null) {
                System.out.println("Sorry, unable to find " + fileName);
                return properties;
            }

            // Load the properties file
            properties.load(input);

        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }

}
