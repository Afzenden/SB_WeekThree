package com.promineotech.jeep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.promineotech.jeep.Constants;
import com.promineotech.jeep.service.DefaultJeepSalesService;
import com.promineotech.jeep.service.JeepSalesService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;

import static org.junit.jupiter.params.provider.Arguments.arguments;

import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import com.promineotech.jeep.JeepSales;
import com.promineotech.jeep.entity.Jeep;
import com.promineotech.jeep.entity.JeepModel;

import lombok.Getter;


class FetchJeepTest extends DefaultJeepSalesController {
    static Stream<Arguments> parametersForInvalidInput() {
        return Stream.of(
                arguments("WRANGLER", "*^%*", "Trim contains non-alpha-numeric characters."),
                arguments("WRANGLER", "C".repeat(Constants.TRIM_MAX_LENGTH + 1), "Trim length too long."),
                arguments("INVALID", "Sport", "Model is not enum value.")
        );
    }

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {JeepSales.class})
    @ActiveProfiles("test")
    @Sql(scripts = {"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
            "classpath:flyway/migrations/V1.1__Jeep_Data.sql"}, config = @SqlConfig(encoding = "utf-8"))
    class TestsThatDoNotPolluteTheApplicationContext extends DefaultJeepSalesController {
        @LocalServerPort
        private int serverPort;
        @Autowired
        @Getter

        private TestRestTemplate restTemplate;

        protected String getBaseUri() {
            return String.format("http://localhost:%d/jeeps", serverPort);
        }

        @Test
        void testThatJeepsAreReturnedWhenAValidModelAndTrimAreSupplied() {
            // Given: a valid model, trim and URI
            JeepModel model = JeepModel.WRANGLER;
            String trim = "Sport";
            String uri = String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
            System.out.println(uri);

            // When:
            ResponseEntity<List<Jeep>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });

            // Then:
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            // And: the actual list returned is the same as the expected list
            List<Jeep> actual = response.getBody();
            List<Jeep> expected = response.getBody();

            assertThat(actual).isEqualTo(expected);
        }


        @Test
        void testThatAnErrorMessageIsReturnedWhenAnUnknownTrimIsSupplied() {
            // Given: a valid model, trim and URI
            JeepModel model = JeepModel.WRANGLER;
            String trim = "Unknown Value";
            String uri = String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
            System.out.println(uri);

            // When:
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });

            // Then:
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

            // And: an error message is returned
            Map<String, Object> error = response.getBody();

            assertThat(error)
                    .containsKey("message")
                    .containsEntry("status code", HttpStatus.NOT_FOUND.value())
                    .containsEntry("uri", "/jeeps")
                    .containsKey("timestamp")
                    .containsEntry("reason", HttpStatus.NOT_FOUND.getReasonPhrase());

        }

        @ParameterizedTest
        @MethodSource("com.promineotech.jeep.controller.FetchJeepTest#parametersForInvalidInput")
        void testThatAnErrorMessageIsReturnedWhenAnInvalidValueIsSupplied(String model, String trim, String reason) {
            // Given: a valid model, trim and URI

            String uri = String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
            System.out.println(uri);

            // When:
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });

            // Then:
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

            // And: an error message is returned
            Map<String, Object> error = response.getBody();

            assertErrorMessageValid(error, HttpStatus.BAD_REQUEST);


        }
    }

    @Nested
    @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = {JeepSales.class})
    @ActiveProfiles("test")
    @Sql(scripts = {"classpath:flyway/migrations/V1.0__Jeep_Schema.sql",
            "classpath:flyway/migrations/V1.1__Jeep_Data.sql"}, config = @SqlConfig(encoding = "utf-8"))
    class TestsThatPolluteTheApplicationContext extends DefaultJeepSalesController {
        @MockBean
        DefaultJeepSalesService defaultJeepSalesService;
        @LocalServerPort
        private int serverPort;
        @Autowired
        @Getter

        private TestRestTemplate restTemplate;

        protected String getBaseUri() {
            return String.format("http://localhost:%d/jeeps", serverPort);
        }

        @Test
        void testThatAnUnplannedErrorResultsInA500Status() {
            // Given: a valid model, trim and URI
            JeepModel model = JeepModel.WRANGLER;
            String trim = "Invalid";
            String uri = String.format("%s?model=%s&trim=%s", getBaseUri(), model, trim);
            System.out.println(uri);

            Mockito.doThrow(new RuntimeException("Ouch!")).when(defaultJeepSalesService).fetchJeeps(model, trim);

            // When:
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(uri, HttpMethod.GET, null,
                    new ParameterizedTypeReference<>() {
                    });

            // Then: an internal server error (500) status is returned
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

            // And: an error message is returned
            Map<String, Object> error = response.getBody();

            assertThat(error)
                    .containsKey("message")
                    .containsEntry("status code", HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .containsEntry("uri", "/jeeps")
                    .containsKey("timestamp")
                    .containsEntry("reason", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());

        }
    }
}