package org.carlspring.strongbox.rest;


import org.carlspring.strongbox.config.WebConfig;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ProxyConfiguration;
import org.carlspring.strongbox.storage.Storage;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.routing.RoutingRule;
import org.carlspring.strongbox.storage.routing.RuleSet;
import org.carlspring.strongbox.xml.parsers.GenericParser;

import javax.inject.Inject;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.HttpServerErrorException;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by yury on 8/9/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = WebConfig.class)
@WebAppConfiguration
@WithUserDetails("admin")
@Rollback(false)
public class ConfigurationManagementControllerTest
        extends BackendBaseTest
{

    public static final String ADD_ACCEPTED_RULE_SET_JSON = "{\n" +
                                                            "  \"rule-set\": {\n" +
                                                            "    \"group-repository\": \"group-releases-2\",\n" +
                                                            "    \"rule\": [\n" +
                                                            "      {\n" +
                                                            "        \"pattern\": \".*some.test\",\n" +
                                                            "        \"repository\": [\n" +
                                                            "          \"releases-with-trash\",\n" +
                                                            "          \"releases-with-redeployment\"\n" +
                                                            "        ]\n" +
                                                            "      }\n" +
                                                            "    ]\n" +
                                                            "  }\n" +
                                                            "}";
    public static final String ADD_ACCEPTED_REPO_JSON = "{\n" +
                                                        "  \"rule\": {\n" +
                                                        "    \"pattern\": \".*some.test\",\n" +
                                                        "    \"repository\": [\n" +
                                                        "      \"releases2\",\n" +
                                                        "      \"releases3\"\n" +
                                                        "    ]\n" +
                                                        "  }\n" +
                                                        "}";
    public static final String OVERRIDE_REPO_JSON = "{\n" +
                                                    "          \"rule\":\n" +
                                                    "            {\n" +
                                                    "              \"pattern\": \".*some.test\",\n" +
                                                    "              \"repository\": [\n" +
                                                    "                \"releases22\", \"releases32\"\n" +
                                                    "              ]\n" +
                                                    "            }\n" +
                                                    "\n" +
                                                    "}";

    @Inject
    ObjectMapper objectMapper;

    @Test
    public void testSetAndGetPort()
            throws Exception
    {
        int newPort = 18080;

        String url = getContextBaseUrl() + "/configuration/strongbox/port/" + newPort;

        int status = RestAssuredMockMvc.given()
                                       .contentType(MediaType.APPLICATION_JSON_VALUE)
                                       .when()
                                       .put(url)
                                       .then()
                                       .statusCode(200) // check http status code
                                       .extract()
                                       .statusCode();

        url = getContextBaseUrl() + "/configuration/strongbox/port";

        String port = RestAssuredMockMvc.given()
                                        .contentType(MediaType.APPLICATION_JSON_VALUE)
                                        .when()
                                        .get(url)
                                        .then()
                                        .statusCode(200) // check http status code
                                        .extract().asString();

        assertEquals("Failed to set port!", 200, status);
        assertEquals("Failed to get port!", newPort, Integer.parseInt(port));

    }

    @Test
    public void testSetAndGetBaseUrl()
            throws Exception
    {
        String baseUrl = "http://localhost:" + 40080 + "/newurl";

        String url = getContextBaseUrl() + "/configuration/strongbox/baseUrl";

        RestAssuredMockMvc.given()
                          .contentType(MediaType.TEXT_PLAIN_VALUE)
                          .body(baseUrl)
                          .when()
                          .put(url)
                          .then()
                          .statusCode(200)
                          .extract();


        url = getContextBaseUrl() + "/configuration/strongbox/baseUrl";

        RestAssuredMockMvc.given()
                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                          .when()
                          .get(url)
                          .then()
                          .statusCode(200)
                          .body("baseUrl", equalTo(baseUrl));

    }

    @Test
    public void testSetAndGetGlobalProxyConfiguration()
            throws Exception
    {
        ProxyConfiguration proxyConfiguration = createProxyConfiguration();
        GenericParser<ProxyConfiguration> parser = new GenericParser<>(ProxyConfiguration.class);
        String serializedConfig = parser.serialize(proxyConfiguration);

        logger.info("Serialized config -> \n" + serializedConfig);

        String url = getContextBaseUrl() + "/configuration/strongbox/proxy-configuration";

        RestAssuredMockMvc.given()
                          .contentType(MediaType.TEXT_PLAIN_VALUE)
                          .body(serializedConfig)
                          .when()
                          .put(url)
                          .then()
                          .statusCode(200);

        url = getContextBaseUrl() + "/configuration/strongbox/proxy-configuration";

        System.out.println(proxyConfiguration.getHost() + "++++++++");

        String response = RestAssuredMockMvc.given()
                                            .contentType(MediaType.APPLICATION_XML_VALUE)
                                            .when()
                                            .get(url)
                                            .then()
                                            .statusCode(200)
                                            .extract().response().getBody().asString();

        GenericParser<ProxyConfiguration> parser2 = new GenericParser<>(ProxyConfiguration.class);
        ProxyConfiguration pc = parser2.deserialize(response);

        assertNotNull("Failed to get proxy configuration!", pc);
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getHost(), pc.getHost());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getPort(), pc.getPort());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getUsername(), pc.getUsername());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getPassword(), pc.getPassword());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getType(), pc.getType());
        assertEquals("Failed to get proxy configuration!", proxyConfiguration.getNonProxyHosts(),
                     pc.getNonProxyHosts());
    }


    @Test
    @WithUserDetails("admin")
    public void testAddGetStorage()
            throws Exception
    {
        String storageId = "storage1";

        Storage storage1 = new Storage("storage1");

        String url = getContextBaseUrl() + "/configuration/strongbox/storages";

        GenericParser<Storage> parser = new GenericParser<>(Storage.class);
        String serializedStorage = parser.serialize(storage1);

        RestAssuredMockMvc.given()
                          .contentType(MediaType.TEXT_PLAIN_VALUE)
                          .body(serializedStorage)
                          .when()
                          .put(url)
                          .then()
                          .statusCode(200);

        Repository r1 = new Repository("repository0");
        r1.setAllowsRedeployment(true);
        r1.setSecured(true);
        r1.setStorage(storage1);

        Repository r2 = new Repository("repository1");
        r2.setAllowsForceDeletion(true);
        r2.setTrashEnabled(true);
        r2.setStorage(storage1);
        r2.setProxyConfiguration(createProxyConfiguration());


        addRepository(r1);
        addRepository(r2);

        Storage storage = getStorage(storageId);

        assertNotNull("Failed to get storage (" + storageId + ")!", storage);
        assertFalse("Failed to get storage (" + storageId + ")!", storage.getRepositories().isEmpty());
        assertTrue("Failed to get storage (" + storageId + ")!",
                   storage.getRepositories().get("repository0").allowsRedeployment());
        assertTrue("Failed to get storage (" + storageId + ")!",
                   storage.getRepositories().get("repository0").isSecured());
        assertTrue("Failed to get storage (" + storageId + ")!",
                   storage.getRepositories().get("repository1").allowsForceDeletion());
        assertTrue("Failed to get storage (" + storageId + ")!",
                   storage.getRepositories().get("repository1").isTrashEnabled());

        assertNotNull("Failed to get storage (" + storageId + ")!",
                      storage.getRepositories().get("repository1").getProxyConfiguration().getHost());
        assertEquals("Failed to get storage (" + storageId + ")!",
                     "localhost",
                     storage.getRepositories().get("repository1").getProxyConfiguration().getHost());
    }

    public Storage getStorage(String storageId)
            throws IOException, JAXBException
    {

        String url = getContextBaseUrl() + "/configuration/strongbox/storages/" + storageId;

        String response = RestAssuredMockMvc.given()
                                            .contentType(MediaType.TEXT_PLAIN_VALUE)
                                            .when()
                                            .get(url)
                                            .then()
                                            .statusCode(200)
                                            .extract().response().getBody().asString();

        GenericParser<Storage> parser2 = new GenericParser<>(Storage.class);
        Storage storage = parser2.deserialize(response);
        return storage;
    }

    public int addRepository(Repository repository)
            throws IOException, JAXBException
    {
        String url;
        if (repository == null)
        {
            logger.error("Unable to add non-existing repository.");
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                               "Unable to add non-existing repository.");
        }

        if (repository.getStorage() == null)
        {
            logger.error("Storage associated with repo is null.");
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR,
                                               "Storage associated with repo is null.");
        }

        try
        {
            url = getContextBaseUrl() + "/configuration/strongbox/storages/" + repository.getStorage().getId() + "/" +
                  repository.getId();
        }
        catch (RuntimeException e)
        {
            logger.error("Unable to create web resource.", e);
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        GenericParser<Repository> parser = new GenericParser<>(Repository.class);
        String serializedRepository = parser.serialize(repository);

        int status = RestAssuredMockMvc.given()
                                       .contentType(MediaType.TEXT_PLAIN_VALUE)
                                       .body(serializedRepository)
                                       .when()
                                       .put(url)
                                       .then()
                                       .statusCode(200)
                                       .extract().statusCode();

        return status;
    }

    @Test
    @WithUserDetails("admin")
    @Ignore
    /*
        This test is ignored because index hashTable that is used from server side is empty and
        never populated with any data. Functionality of indexes is not ready for testing yet.
     */
    public void testCreateAndDeleteStorage()
            throws IOException, JAXBException
    {
        final String storageId = "storage2";
        final String repositoryId1 = "repository0";
        final String repositoryId2 = "repository1";

        Storage storage2 = new Storage(storageId);

        String url = getContextBaseUrl() + "/configuration/strongbox/storages";

        GenericParser<Storage> parser = new GenericParser<>(Storage.class);
        String serializedStorage = parser.serialize(storage2);

        RestAssuredMockMvc.given()
                          .contentType(MediaType.TEXT_PLAIN_VALUE)
                          .body(serializedStorage)
                          .when()
                          .put(url)
                          .peek() // Use peek() to print the ouput
                          .then()
                          .statusCode(200);

        Repository r1 = new Repository(repositoryId1);
        r1.setAllowsRedeployment(true);
        r1.setSecured(true);
        r1.setStorage(storage2);
        r1.setProxyConfiguration(createProxyConfiguration());

        Repository r2 = new Repository(repositoryId2);
        r2.setAllowsRedeployment(true);
        r2.setSecured(true);
        r2.setStorage(storage2);

        addRepository(r1);
        addRepository(r2);

        url = getContextBaseUrl() + "/configuration/strongbox/proxy-configuration";

        RestAssuredMockMvc.given()
                          .contentType(MediaType.APPLICATION_XML_VALUE)
                          .params("storageId", storageId, "repositoryId", repositoryId1)
                          .when()
                          .get(url)
                          .peek() // Use peek() to print the ouput
                          .then()
                          .statusCode(200)
                          .extract();

        Storage storage = getStorage(storageId);

        assertNotNull("Failed to get storage (" + storageId + ")!", storage);
        assertFalse("Failed to get storage (" + storageId + ")!", storage.getRepositories().isEmpty());

        //    response = client.deleteRepository(storageId, repositoryId1, true);

        url = getContextBaseUrl() + "/configuration/strongbox/storages/" + storageId + "/" + repositoryId1;
        logger.debug(url);

        RestAssuredMockMvc.given()
                          .contentType(MediaType.TEXT_PLAIN_VALUE)
                          .param("force", true)
                          .when()
                          .delete(url)
                          .peek() // Use peek() to print the ouput
                          .then()
                          .statusCode(200);

        url = getContextBaseUrl() + "/configuration/strongbox/storages/" + storageId + "/" + repositoryId1;

        logger.debug(storageId);
        logger.debug(repositoryId1);

        RestAssuredMockMvc.given()
                          .contentType(MediaType.TEXT_PLAIN_VALUE)
                          .when()
                          .get(url)
                          .peek() // Use peek() to print the ouput
                          .then()
                          .statusCode(404);
    }

    @Test
    @WithUserDetails("admin")
    public void testGetAndSetConfiguration()
            throws IOException, JAXBException
    {
        Configuration configuration = getConfiguration();

        Storage storage = new Storage("storage3");

        configuration.addStorage(storage);

        String url = getContextBaseUrl() + "/configuration/strongbox/xml";

        GenericParser<Configuration> parser2 = new GenericParser<>(Configuration.class);
        String serializedConfiguration = parser2.serialize(configuration);

        RestAssuredMockMvc.given()
                          .contentType(MediaType.TEXT_PLAIN_VALUE)
                          .body(serializedConfiguration)
                          .when()
                          .put(url)
                          .then()
                          .statusCode(200);

        final Configuration c = getConfiguration();
        Assert.assertNotNull("Failed to create storage3!", c.getStorage("storage3"));

    }


    public Configuration getConfiguration()
            throws IOException, JAXBException
    {
        String url = getContextBaseUrl() + "/configuration/strongbox/xml";

        String response = RestAssuredMockMvc.given()
                                            .contentType(MediaType.TEXT_PLAIN_VALUE)
                                            .when()
                                            .get(url)
                                            .then()
                                            .statusCode(200)
                                            .extract().response().getBody().asString();

        GenericParser<Configuration> parser = new GenericParser<>(Configuration.class);
        Configuration configuration = parser.deserialize(response);

        return configuration;

    }

    @Test
    public void addAcceptedRuleSet()
            throws Exception
    {
        acceptedRuleSet();
    }

    @Test
    public void removeAcceptedRuleSet()
            throws Exception
    {
        acceptedRuleSet();

        String url = getContextBaseUrl() + "/configuration/strongbox/routing/rules/set/accepted/group-releases-2";

        RestAssuredMockMvc.given()
                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                          .when()
                          .delete(url)
                          .then()
                          .statusCode(200);
    }

    @Test
    public void addAcceptedRepository()
            throws Exception
    {
        acceptedRuleSet();
        acceptedRepository();
    }

    @Test
    public void removeAcceptedRepository()
            throws Exception
    {
        acceptedRuleSet();
        acceptedRepository();

        String url = getContextBaseUrl() +
                     "/configuration/strongbox/routing/rules/accepted/group-releases-2/repositories/releases3?pattern=.*some.test";

        RestAssuredMockMvc.given()
                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                          .when()
                          .delete(url)
                          .then()
                          .statusCode(200);

    }

    @Test
    public void overrideAcceptedRepository()
            throws Exception
    {
        acceptedRuleSet();
        acceptedRepository();

        String url = getContextBaseUrl() +
                     "/configuration/strongbox/routing/rules/accepted/group-releases-2/override/repositories";

        RoutingRule routingRule = new RoutingRule();
        routingRule.setPattern(".*some.test");
        Set<String> repositories = new HashSet<String>();
        repositories.add("releases22");
        repositories.add("releases32");
        routingRule.setRepositories(repositories);

        GenericParser<RoutingRule> parser = new GenericParser<>(RoutingRule.class);
        String serialezeRoutingRule = null;
        try
        {
            serialezeRoutingRule = parser.serialize(routingRule);
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
        }

        RestAssuredMockMvc.given()
                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                          .body(serialezeRoutingRule)
                          .when()
                          .put(url)
                          .then()
                          .statusCode(200);
    }

    private void acceptedRuleSet()
            throws IOException
    {
        String url = getContextBaseUrl() + "/configuration/strongbox/routing/rules/set/accepted";

        RuleSet ruleSet = new RuleSet();
        ruleSet.setGroupRepository("group-releases-2");
        RoutingRule routingRule = new RoutingRule();
        routingRule.setPattern(".*some.test");
        Set<String> repositories = new HashSet<String>();
        repositories.add("releases-with-trash");
        repositories.add("releases-with-redeployment");
        routingRule.setRepositories(repositories);

        List<RoutingRule> rule = new LinkedList<>();
        rule.add(routingRule);
        ruleSet.setRoutingRules(rule);

        GenericParser<RuleSet> parser = new GenericParser<>(RuleSet.class);
        String serializeRuleSet = null;
        try
        {
            serializeRuleSet = parser.serialize(ruleSet);
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
        }

        RestAssuredMockMvc.given()
                          .contentType(MediaType.TEXT_PLAIN_VALUE)
                          .body(serializeRuleSet)
                          .when()
                          .put(url)
                          .then()
                          .statusCode(200);
    }

    private void acceptedRepository()
            throws IOException
    {
        String url =
                getContextBaseUrl() + "/configuration/strongbox/routing/rules/accepted/group-releases-2/repositories";

        RoutingRule routingRule = new RoutingRule();
        routingRule.setPattern(".*some.test");
        Set<String> repositories = new HashSet<String>();
        repositories.add("releases2");
        repositories.add("releases3");
        routingRule.setRepositories(repositories);

        GenericParser<RoutingRule> parser = new GenericParser<>(RoutingRule.class);
        String serialezeRoutingRule = null;
        try
        {
            serialezeRoutingRule = parser.serialize(routingRule);
        }
        catch (JAXBException e)
        {
            e.printStackTrace();
        }

        RestAssuredMockMvc.given()
                          .contentType(MediaType.TEXT_PLAIN_VALUE)
                          .body(serialezeRoutingRule)
                          .when()
                          .put(url)
                          .then()
                          .statusCode(200);
    }

    private ProxyConfiguration createProxyConfiguration()
    {
        ProxyConfiguration proxyConfiguration = new ProxyConfiguration();
        proxyConfiguration.setHost("localhost");
        proxyConfiguration.setPort(8080);
        proxyConfiguration.setUsername("user1");
        proxyConfiguration.setPassword("pass2");
        proxyConfiguration.setType("http");
        List<String> nonProxyHosts = new ArrayList<>();
        nonProxyHosts.add("localhost");
        nonProxyHosts.add("some-hosts.com");
        proxyConfiguration.setNonProxyHosts(nonProxyHosts);

        return proxyConfiguration;
    }

}
