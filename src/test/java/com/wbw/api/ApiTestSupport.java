package com.wbw.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbw.document.DocumentRepository;
import com.wbw.folder.FolderRepository;
import com.wbw.settings.UserSettingsRepository;
import com.wbw.share.ShareRepository;
import com.wbw.user.User;
import com.wbw.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestSupport {

    protected static final String ALICE = "google-sub-alice";
    protected static final String BOB = "google-sub-bob";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected FolderRepository folderRepository;
    @Autowired protected DocumentRepository documentRepository;
    @Autowired protected ShareRepository shareRepository;
    @Autowired protected UserSettingsRepository settingsRepository;

    @BeforeEach
    void resetData() {
        shareRepository.deleteAllInBatch();
        documentRepository.deleteAllInBatch();
        folderRepository.deleteAllInBatch();
        settingsRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        userRepository.save(new User(ALICE, "alice@example.com", "Alice", null));
        userRepository.save(new User(BOB, "bob@example.com", "Bob", null));
    }

    protected RequestPostProcessor as(String sub) {
        return oidcLogin().idToken(token -> token.subject(sub));
    }

    protected String json(Object body) throws Exception {
        return mapper.writeValueAsString(body);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsByteArray());
    }

    protected MockHttpServletRequestBuilder getAs(String sub, String url, Object... vars) {
        return get(url, vars).with(as(sub));
    }

    protected MockHttpServletRequestBuilder postAs(String sub, String url, Object body, Object... vars) throws Exception {
        return post(url, vars).with(as(sub)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    protected MockHttpServletRequestBuilder putAs(String sub, String url, Object body, Object... vars) throws Exception {
        return put(url, vars).with(as(sub)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    protected MockHttpServletRequestBuilder patchAs(String sub, String url, Object body, Object... vars) throws Exception {
        return patch(url, vars).with(as(sub)).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json(body));
    }

    protected MockHttpServletRequestBuilder deleteAs(String sub, String url, Object... vars) {
        return delete(url, vars).with(as(sub)).with(csrf());
    }

    /** 문단 하나짜리 TipTap 문서 */
    protected JsonNode doc(String text) throws Exception {
        String json = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":"
                + mapper.writeValueAsString(text) + "}]}]}";
        return mapper.readTree(json);
    }
}
