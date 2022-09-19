package dojo.liftpasspricing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.*;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import spark.Spark;

class PricesTest {

    private static final int PORT = 4568;
    private static Connection connection;

    @BeforeAll
    static void createPrices() throws SQLException {
        connection = Prices.createApp(PORT);
    }

    @AfterAll
    static void stopApplication() throws SQLException {
        Spark.stop();
        connection.close();
    }

    @Test
    void childrenShouldNeverPay() {
        checkReturnedPrice("5", "1day", 0);
        checkReturnedPrice("5", "night", 0);
        checkReturnedPrice("5", null, 0);
        checkReturnedPrice("5", "", 0);
        checkReturnedPrice("1", "night", 0);
        checkReturnedPrice("0", "night", 0);
        checkReturnedPrice("-2", "night", 0);
    }

    @Test
    void whenAgeIsNotSpecifiedAndByNightItSFree() {
        checkReturnedPrice(null, "night", 0);
    }

    @Test
    void byNightSeniorsShouldHaveADiscount() {
        checkReturnedPrice("65", "night", 8);
        checkReturnedPrice("165", "night", 8);
    }

    @Test
    void regularPriceForAnAdultByNight() {
        checkReturnedPrice("6", "night", 19);
        checkReturnedPrice("64", "night", 19);
    }

    private static void checkReturnedPrice(String age, String type, int expected) {
        record Param(String paramName, String paramValue) {
        }

        var params =
                Stream.of(
                        new Param("age", age),
                        new Param("type", type)
                )
                .filter(it -> it.paramValue != null)
                .map(it -> it.paramName + "=" + it.paramValue)
                .collect(Collectors.joining("&", "?", ""));


        String url = "/prices" + params;

        JsonPath response = RestAssured.
                given().
                port(PORT).
                when().
                // construct some proper url parameters
                        get(url).
                then().
                assertThat().
                statusCode(200).
                assertThat().
                contentType("application/json").
                extract().jsonPath();

        assertEquals(expected, response.getInt("cost"));
    }

}
