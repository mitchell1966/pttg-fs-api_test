package apidocs.v1;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.builder.RequestSpecBuilder;
import com.jayway.restassured.http.ContentType;
import com.jayway.restassured.specification.RequestSpecification;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.restassured.RestDocumentationFilter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.gov.digital.ho.proving.financialstatus.api.ServiceRunner;
import uk.gov.digital.ho.proving.financialstatus.api.configuration.ServiceConfiguration;

import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.document;
import static org.springframework.restdocs.restassured.RestAssuredRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.restassured.operation.preprocess.RestAssuredPreprocessors.modifyUris;
import static org.springframework.restdocs.snippet.Attributes.key;

@SpringApplicationConfiguration(classes = {ServiceRunner.class, ServiceConfiguration.class})
@WebIntegrationTest("server.port=0")
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = {
    "barclays.stub.service=http://localhost:8089"
})
public class ThresholdCalculator {

    public static final String BASEPATH = "/pttg/financialstatusservice/v1/";

    @Rule
    public JUnitRestDocumentation restDocumentationRule = new JUnitRestDocumentation("build/generated-snippets");

    @Value("${local.server.port}")
    private int port;

    private RequestSpecification documentationSpec;

    private RequestSpecification requestSpec;

    private RestDocumentationFilter document =
        document("{method-name}",
            preprocessRequest(
                prettyPrint(),
                modifyUris()
                    .scheme("https")
                    .host("api.host.address")
                    .removePort()
            ),
            preprocessResponse(
                prettyPrint(),
                removeHeaders("Date", "Connection", "Transfer-Encoding")
            )
        );


    private FieldDescriptor[] statusModelFields = new FieldDescriptor[]{
        fieldWithPath("status").description("to do - i don't know what this means"),
        fieldWithPath("status.code").description("to do - i don't know what this means"),
        fieldWithPath("status.message").description("to do - i don't know what this means")
    };

    private FieldDescriptor[] bodyModelFields = new FieldDescriptor[]{
        fieldWithPath("threshold").description("minimum daily balance"),
    };

    @Before
    public void setUp() {

        RestAssured.port = this.port;
        RestAssured.basePath = BASEPATH;

        requestSpec = new RequestSpecBuilder()
            .setAccept(ContentType.JSON)
            .build();

        this.documentationSpec =
            new RequestSpecBuilder()
                .addFilter(documentationConfiguration(this.restDocumentationRule))
                .addFilter(document)
                .build();
    }

    @Test
    public void commonHeaders() throws Exception {

        given(documentationSpec)
            .spec(requestSpec)
            .param("inLondon", "true")
            .param("courseLength", "4")
            .param("tuitionFees", "12500")
            .param("tuitionFeesPaid", "250.50")
            .param("accommodationFeesPaid", "300")
            .param("studentType", "nondoctorate")
            .param("dependants", "0")
            .filter(document.snippets(
                requestHeaders(
                    headerWithName("Accept").description("The requested media type eg application/json. See <<Schema>> for supported media types.")
                ),
                responseHeaders(
                    headerWithName("Content-Type").description("The Content-Type of the payload, e.g. `application/json`")
                )
            ))

            .when().get("/maintenance/threshold")
            .then().assertThat().statusCode(is(200));
    }

    @Test
    public void thresholdCalculation() throws Exception {

        given(documentationSpec)
            .spec(requestSpec)
            .param("inLondon", "true")
            .param("courseLength", "4")
            .param("tuitionFees", "12500")
            .param("tuitionFeesPaid", "250.50")
            .param("accommodationFeesPaid", "300")
            .param("studentType", "nondoctorate")
            .param("dependants", "1")
             .filter(document.snippets(
                responseFields(bodyModelFields)
                    .and(statusModelFields),
                requestParameters(
                    parameterWithName("inLondon")
                        .description("whether the location is an in London")
                        .attributes(key("optional").value(false)),
                    parameterWithName("courseLength")
                        .description("the length of the course in months (not required for 'doctorate' student type)")
                        .attributes(key("optional").value(true)),
                    parameterWithName("tuitionFees")
                        .description("total tuition fees (not required for 'doctorate' student type)")
                        .attributes(key("optional").value(true)),
                    parameterWithName("tuitionFeesPaid")
                        .description("tuition fees already paid (not required for 'doctorate' student type)")
                        .attributes(key("optional").value(true)),
                    parameterWithName("accommodationFeesPaid")
                        .description("accommodation fees already paid")
                        .attributes(key("optional").value(false)),
                    parameterWithName("studentType")
                        .description("type of student, current possible values are 'doctorate', 'nondoctorate', 'pgdd' and 'sso'")
                        .attributes(key("optional").value(false)),
                    parameterWithName("dependants")
                        .description("the number of dependants to take in to account when calculating the minimum balance")
                        .attributes(key("optional").value(true))
                )

            ))

            .when().get("/maintenance/threshold")
            .then().assertThat().statusCode(is(200));
    }


    @Test
    public void missingParameterError() throws Exception {

        given(documentationSpec)
            .spec(requestSpec)
            .filter(document.snippets(
                responseFields(
                    fieldWithPath("status.code").description("A specific error code to identify further details of this error"),
                    fieldWithPath("status.message").description("A description of the error, in this case identifying the missing mandatory parameter")
                )
            ))

            .when().get("/maintenance/threshold")
            .then().assertThat().statusCode(is(400));
    }


}