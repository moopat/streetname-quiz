package at.trycatch.extractor;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.geojson.Feature;
import org.geojson.FeatureCollection;
import org.geojson.Point;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Extractor {

    public static void main(String[] args) throws Exception {

        final URL url = Extractor.class.getClassLoader().getResource("graz-streetnames.csv");
        final Stream<String> streetStream = Files.lines(Paths.get(url.toURI()), Charset.forName("ISO-8859-1"));

        final List<String> officialStreets = streetStream.map(s -> {
            final String[] split = s.split(";");
            if (split.length < 2) return "";
            return split[1];
        }).collect(Collectors.toList());

        final List<String> officialNormalizedStreets = officialStreets.stream().map(Extractor::getNormalizedStreet).collect(Collectors.toList());

        // Remove the first street as it is the heading.
        officialStreets.remove(0);

        final List<String> missingStreets = new ArrayList<>(officialNormalizedStreets);

        System.out.println("Found " + officialStreets.size() + " official streets.");


        final InputStream stream = Extractor.class.getClassLoader().getResourceAsStream("graz-raw.geojson");
        final FeatureCollection features = new ObjectMapper().readValue(stream, FeatureCollection.class);


        final List<Feature> eligibleFeatures = features.getFeatures().stream()
                .filter(feature -> feature.getProperties().containsKey("name"))
                .filter(feature -> !feature.getProperties().containsKey("addr:housenumber"))
                .filter(feature -> !(feature.getGeometry() instanceof Point))
                .filter(feature -> officialNormalizedStreets.contains(getNormalizedStreet((String) feature.getProperties().get("name"))))
                .collect(Collectors.toList());

        System.out.println("Found " + eligibleFeatures.size() + " eligible features that will be included in the app.");

        AtomicInteger count = new AtomicInteger();
        eligibleFeatures.stream()
                .map(feature -> (String) feature.getProperties().get("name"))
                .map(Extractor::getNormalizedStreet)
                .distinct()
                .forEach(it -> {
                    missingStreets.remove(it);
                    count.getAndIncrement();
                });

        System.out.println("-------------");
        System.out.println("Found " + count.get() + " matching streets in the open dataset.");

        System.out.println("\n\n\n Found " + missingStreets.size() + " missing streets.\n");

        missingStreets.forEach(System.out::println);

        // Write feature collection
        final FeatureCollection newFeatureCollection = new FeatureCollection();
        newFeatureCollection.addAll(eligibleFeatures);
        new ObjectMapper().writeValue(new File("graz.json"), newFeatureCollection);

        // Write streets collection
        FileWriter writer = new FileWriter("grazstreets.txt");
        officialStreets.stream()
                .filter(s -> !missingStreets.contains(getNormalizedStreet(s)))
                .forEach(s -> {
                    try {
                        writer.write(s + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        writer.close();
    }

    public static String getNormalizedStreet(final String street) {
        return street.toLowerCase()
                .replace("st.", "sankt")
                .replace("prof.", "professor")
                .replace(".", "")
                .replace(" ", "")
                .replace("-", "");
    }

}
