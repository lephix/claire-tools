package com.lephix.clairetools;

import com.lephix.clairetools.command.CollectPODTracking;
import com.lephix.clairetools.command.TermLanguageFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * The entrance of the program.
 * <p/>
 * Created by longxiang on 16/1/21.
 */
@EnableAutoConfiguration
public class Application implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    Environment environment;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            if (!environment.getProperty("application.start", boolean.class)) {
                LOG.info("application.start is set to false. Exit.");
                return;
            }
        } catch (Exception e) {
            LOG.error("Please check the command line.");
            return;
        }


        String commandName = environment.getProperty("command.name").trim();
        if (StringUtils.isEmpty(commandName)) {
            LOG.info("No command.name configuration found. Exit.");
            return;
        }

        try {
            switch (commandName) {
                case "collectPODTracking":
                    CollectPODTracking collectPODTracking = new CollectPODTracking(environment);
                    collectPODTracking.call();
                    break;
                case "termLanguageFinding":
                    TermLanguageFinding termLanguageFinding = new TermLanguageFinding(environment);
                    termLanguageFinding.call();
                    break;
                default:
                    LOG.info("No command.name matches. Exit.");
            }
            LOG.info("{} command has been successfully executed.", commandName);
        } catch (Exception e) {
            LOG.error("Some error happens. See details.", e);
        }
    }

    @Autowired
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
