package org.openmetadata.service.resources.apis;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.openmetadata.service.Entity.FIELD_OWNER;
import static org.openmetadata.service.resources.topics.TopicResourceTest.getField;
import static org.openmetadata.service.util.EntityUtil.fieldUpdated;
import static org.openmetadata.service.util.TestUtils.ADMIN_AUTH_HEADERS;
import static org.openmetadata.service.util.TestUtils.UpdateType.MINOR_UPDATE;
import static org.openmetadata.service.util.TestUtils.assertListNotNull;
import static org.openmetadata.service.util.TestUtils.assertListNull;
import static org.openmetadata.service.util.TestUtils.assertResponse;
import static org.openmetadata.service.util.TestUtils.validateEntityReference;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.apache.http.client.HttpResponseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openmetadata.schema.api.data.CreateAPIEndpoint;
import org.openmetadata.schema.entity.data.APIEndpoint;
import org.openmetadata.schema.type.APIRequestMethod;
import org.openmetadata.schema.type.APISchema;
import org.openmetadata.schema.type.ChangeDescription;
import org.openmetadata.schema.type.EntityReference;
import org.openmetadata.schema.type.Field;
import org.openmetadata.schema.type.FieldDataType;
import org.openmetadata.service.Entity;
import org.openmetadata.service.resources.EntityResourceTest;
import org.openmetadata.service.util.FullyQualifiedName;
import org.openmetadata.service.util.ResultList;
import org.openmetadata.service.util.TestUtils;

public class APIEndpointResourceTest extends EntityResourceTest<APIEndpoint, CreateAPIEndpoint> {

  public static final List<Field> api_request_fields =
      Arrays.asList(
          getField("name", FieldDataType.STRING, null), getField("q", FieldDataType.STRING, null));
  public static final List<Field> api_response_fields =
      Arrays.asList(
          getField("id", FieldDataType.STRING, null),
          getField("first_name", FieldDataType.STRING, null),
          getField("last_name", FieldDataType.STRING, null),
          getField("email", FieldDataType.STRING, null),
          getField("address_line_1", FieldDataType.STRING, null),
          getField("address_line_2", FieldDataType.STRING, null),
          getField("post_code", FieldDataType.STRING, null),
          getField("county", FieldDataType.STRING, PERSONAL_DATA_TAG_LABEL));

  public static final APISchema REQUEST_SCHEMA =
      new APISchema().withSchemaFields(api_request_fields);
  public static final APISchema RESPONSE_SCHEMA =
      new APISchema().withSchemaFields(api_response_fields);

  public APIEndpointResourceTest() {
    super(
        Entity.API_ENDPOINT,
        APIEndpoint.class,
        APIEndpointResource.APIEndpointList.class,
        "apiEndpoints",
        APIEndpointResource.FIELDS);
    supportsSearchIndex = true;
  }

  @Test
  void post_apiEndpointWithoutRequiredFields_4xx(TestInfo test) {
    // Service is required field
    assertResponse(
        () -> createEntity(createRequest(test).withApiCollection(null), ADMIN_AUTH_HEADERS),
        BAD_REQUEST,
        "[apiCollection must not be null]");

    // Partitions is required field
    assertResponse(
        () -> createEntity(createRequest(test).withEndpointURL(null), ADMIN_AUTH_HEADERS),
        BAD_REQUEST,
        "[endpointURL must not be null]");
  }

  @Test
  void post_apiEndpointWithDifferentService_200_ok(TestInfo test) throws IOException {
    String[] differentAPICollections = {
      OPENMETADATA_API_COLLECTION_REFERENCE.getFullyQualifiedName(),
      SAMPLE_API_COLLECTION_REFERENCE.getFullyQualifiedName()
    };

    // Create topic for each service and test APIs
    for (String apiCollection : differentAPICollections) {
      createAndCheckEntity(
          createRequest(test).withApiCollection(apiCollection), ADMIN_AUTH_HEADERS);

      // List topics by filtering on service name and ensure right endPoints in the response
      Map<String, String> queryParams = new HashMap<>();
      queryParams.put("apiCollection", apiCollection);

      ResultList<APIEndpoint> list = listEntities(queryParams, ADMIN_AUTH_HEADERS);
      for (APIEndpoint endpoint : list.getData()) {
        assertEquals(apiCollection, endpoint.getApiCollection().getFullyQualifiedName());
      }
    }
  }

  @Test
  void put_endPointAttributes_200_ok(TestInfo test) throws IOException {
    APISchema responseSchema = new APISchema().withSchemaFields(api_response_fields);
    CreateAPIEndpoint createAPIEndpoint =
        createRequest(test)
            .withOwner(USER1_REF)
            .withRequestMethod(APIRequestMethod.GET)
            .withEndpointURL(URI.create("https://localhost:8585/api/v1/users"))
            .withResponseSchema(responseSchema);

    // Patch and update the topic
    APIEndpoint apiEndpoint = createEntity(createAPIEndpoint, ADMIN_AUTH_HEADERS);
    createAPIEndpoint
        .withOwner(TEAM11_REF)
        .withResponseSchema(responseSchema)
        .withRequestMethod(APIRequestMethod.POST);

    ChangeDescription change = getChangeDescription(apiEndpoint, MINOR_UPDATE);
    fieldUpdated(change, FIELD_OWNER, USER1_REF, TEAM11_REF);
    fieldUpdated(change, "requestMethod", "GET", "POST");

    updateAndCheckEntity(
        createAPIEndpoint, Response.Status.OK, ADMIN_AUTH_HEADERS, MINOR_UPDATE, change);
  }

  @Override
  public CreateAPIEndpoint createRequest(String name) {
    return new CreateAPIEndpoint()
        .withName(name)
        .withApiCollection(getContainer().getFullyQualifiedName())
        .withRequestMethod(APIRequestMethod.GET)
        .withEndpointURL(URI.create("https://localhost:8585/api/v1/users"));
  }

  @Override
  public EntityReference getContainer() {
    return OPENMETADATA_API_COLLECTION_REFERENCE;
  }

  @Override
  public EntityReference getContainer(APIEndpoint entity) {
    return entity.getApiCollection();
  }

  @Override
  public void validateCreatedEntity(
      APIEndpoint apiEndpoint, CreateAPIEndpoint createRequest, Map<String, String> authHeaders)
      throws HttpResponseException {
    assertEquals(createRequest.getRequestMethod(), apiEndpoint.getRequestMethod());
    validateEntityReference(apiEndpoint.getApiCollection());
    validateEntityReference(apiEndpoint.getService());
    assertReference(createRequest.getApiCollection(), apiEndpoint.getApiCollection());
    TestUtils.validateTags(createRequest.getTags(), apiEndpoint.getTags());
    assertEquals(
        FullyQualifiedName.add(createRequest.getApiCollection(), createRequest.getName()),
        apiEndpoint.getFullyQualifiedName());
  }

  @Override
  public void compareEntities(
      APIEndpoint expected, APIEndpoint updated, Map<String, String> authHeaders)
      throws HttpResponseException {
    assertReference(expected.getService(), expected.getService());
    TestUtils.validateTags(expected.getTags(), updated.getTags());
  }

  @Override
  public void assertFieldChange(String fieldName, Object expected, Object actual) {
    if (expected == actual) {
      return;
    }
    assertCommonFieldChange(fieldName, expected, actual);
  }

  @Override
  public APIEndpoint validateGetWithDifferentFields(APIEndpoint endpoint, boolean byName)
      throws HttpResponseException {
    // .../topics?fields=owner
    String fields = "";
    endpoint =
        byName
            ? getAPIEndpointByName(endpoint.getFullyQualifiedName(), fields, ADMIN_AUTH_HEADERS)
            : getAPIEndpoint(endpoint.getId(), fields, ADMIN_AUTH_HEADERS);
    assertListNull(endpoint.getOwner(), endpoint.getFollowers());

    fields = "owner, followers, tags";
    endpoint =
        byName
            ? getAPIEndpointByName(endpoint.getFullyQualifiedName(), fields, ADMIN_AUTH_HEADERS)
            : getAPIEndpoint(endpoint.getId(), fields, ADMIN_AUTH_HEADERS);
    assertListNotNull(endpoint.getService(), endpoint.getServiceType());
    // Checks for other owner, tags, and followers is done in the base class
    return endpoint;
  }

  public APIEndpoint getAPIEndpoint(UUID id, String fields, Map<String, String> authHeaders)
      throws HttpResponseException {
    WebTarget target = getResource(id);
    target = fields != null ? target.queryParam("fields", fields) : target;
    return TestUtils.get(target, APIEndpoint.class, authHeaders);
  }

  public APIEndpoint getAPIEndpointByName(
      String fqn, String fields, Map<String, String> authHeaders) throws HttpResponseException {
    WebTarget target = getResourceByName(fqn);
    target = fields != null ? target.queryParam("fields", fields) : target;
    return TestUtils.get(target, APIEndpoint.class, authHeaders);
  }

  private static void assertFields(List<Field> expectedFields, List<Field> actualFields)
      throws HttpResponseException {
    if (expectedFields == actualFields) {
      return;
    }
    // Sort columns by name
    assertEquals(expectedFields.size(), actualFields.size());

    // Make a copy before sorting in case the lists are immutable
    List<Field> expected = new ArrayList<>(expectedFields);
    List<Field> actual = new ArrayList<>(actualFields);
    expected.sort(Comparator.comparing(Field::getName));
    actual.sort(Comparator.comparing(Field::getName));
    for (int i = 0; i < expected.size(); i++) {
      assertField(expected.get(i), actual.get(i));
    }
  }

  private static void assertField(Field expectedField, Field actualField)
      throws HttpResponseException {
    assertNotNull(actualField.getFullyQualifiedName());
    assertTrue(
        expectedField.getName().equals(actualField.getName())
            || expectedField.getName().equals(actualField.getDisplayName()));
    assertEquals(expectedField.getDescription(), actualField.getDescription());
    assertEquals(expectedField.getDataType(), actualField.getDataType());
    TestUtils.validateTags(expectedField.getTags(), actualField.getTags());

    // Check the nested columns
    assertFields(expectedField.getChildren(), actualField.getChildren());
  }
}
