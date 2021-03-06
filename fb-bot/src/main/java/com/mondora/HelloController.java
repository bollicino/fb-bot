package com.mondora;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mondora.facebook.StrategyBuilder;
import com.mondora.facebook.Connector;
import com.mondora.facebook.commands.Strategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.PagedList;
import org.springframework.social.facebook.api.Post;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import java.io.IOException;

import static com.mondora.Utils.valid;

@Controller
@RequestMapping("/")
public class HelloController {
    private static final Logger LOG = LoggerFactory.getLogger(HelloController.class);
    private static final String VALIDATION_TOKEN = "disse_la_vacca_al_mulo";

    private Facebook facebook;
    private ConnectionRepository connectionRepository;

    public HelloController(Facebook facebook, ConnectionRepository connectionRepository) {
        this.facebook = facebook;
        this.connectionRepository = connectionRepository;
    }

    @GetMapping
    public String helloFacebook(Model model) {
        LOG.debug("GET /" );
        if (connectionRepository.findPrimaryConnection(Facebook.class) == null) {
            return "redirect:/connect/facebook";
        }

//        b2b.addAttribute("facebookProfile", facebook.userOperations().getUserProfile());
        String[] fields = {"id", "email", "first_name", "last_name"};
        org.springframework.social.facebook.api.User userProfile = facebook.fetchObject("me", org.springframework.social.facebook.api.User.class, fields);

        PagedList<Post> feed = facebook.feedOperations().getFeed();
        model.addAttribute("feed", feed);
        model.addAttribute("fbId", userProfile.getId());
        model.addAttribute("fbName", userProfile.getFirstName() + " " + userProfile.getLastName());
        return "hello";
    }

    @RequestMapping(path = "map", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> map() {
        LOG.debug("GET /map " );
        StringBuffer out = new StringBuffer();
        out.append("{");
        out.append( "'users':{");
        Database.listAllUsers().forEach(o -> out.append(o.toString()));
//        out.append( "}, 'postback':{");
//        Database.listAllPostback().forEach( o-> o + ":\"" + Database.getPostback(o) +"" );
        out.append("}, 'fattura':{");
        Database.listFattura().forEach(o -> out.append( Utils.toJson(o)));
        out.append( "}}");
        return new ResponseEntity<String>(out.toString(), HttpStatus.OK);
    }

    @RequestMapping(path = "deauth", method = RequestMethod.GET)
    public ResponseEntity<String> deAuth(WebRequest json) {
        LOG.debug("GET /deauth " + json.toString());
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @RequestMapping(path = "webhook", method = RequestMethod.GET)
    public ResponseEntity<String> webHook(WebRequest json) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("GET /webhook " + json.toString());
            json.getParameterNames().forEachRemaining(o -> LOG.debug("\t" + o + " " + json.getParameter(o)));
        }
        try {
            if (valid(json, "hub.mode", "subscribe") && valid(json, "hub.verify_token", VALIDATION_TOKEN)) {
                LOG.debug("Validating webhook");
                return new ResponseEntity(json.getParameter("hub.challenge"), HttpStatus.OK);

            } else {
                LOG.info("Failed validation. Make sure the validation tokens match.");
                return new ResponseEntity("Failed validation. Make sure the validation tokens match.", HttpStatus.FORBIDDEN);
            }
        } catch (Exception ex) {
            LOG.error("500 " + ex.getMessage(), ex);
            return new ResponseEntity(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(path = "webhook", method = RequestMethod.POST)
    public ResponseEntity<String> webHookPost(@RequestBody String json) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("POST /webhook " + json);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode node = mapper.readTree(json);
            if (valid(node, "object", "page")) {
                Strategy s = StrategyBuilder.builder(node);
                s.run(node);
            } else {
                LOG.debug("Ignored " + json);
            }
            return new ResponseEntity<String>(HttpStatus.OK);
        } catch (IOException e) {
            LOG.debug("500 " + e.getMessage(), e);
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @RequestMapping(path = "send", method = RequestMethod.POST)
    public ResponseEntity<String> send(@RequestParam String id, @RequestParam String text) {
        Connector.sendTextMessage(id, text);
        return new ResponseEntity<String>(HttpStatus.OK);
    }
}