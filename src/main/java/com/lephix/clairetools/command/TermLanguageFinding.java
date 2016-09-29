package com.lephix.clairetools.command;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Validate;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Created by longxiang on 16/9/28.
 */
public class TermLanguageFinding implements Callable<Boolean> {
    private Logger LOG = LoggerFactory.getLogger(TermLanguageFinding.class);

    private Environment environment;
    private Map<String, Set<String>> resultMap = Maps.newLinkedHashMap();

    private int sourceCol = CellReference.convertColStringToIndex("C");
    private int languageCol = CellReference.convertColStringToIndex("A");
    private int certCol = CellReference.convertColStringToIndex("E");

    public TermLanguageFinding(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Boolean call() throws Exception {
        List<String> termList = FileUtils.readLines(new File(environment.getProperty("source.term.list.path")));
        initResultMap(termList);
        Collection<File> targetFileList = FileUtils.listFiles(
                new File(environment.getProperty("target.folder.path")),
                new String[]{"xlsx"},
                false
        );

        for (File file : targetFileList) {
            Workbook targetWb = null;
            try {
                if (file.getName().startsWith("~")) {
                    // skip temp file.
                    continue;
                }

                LOG.info("Start Processing file {} .", file.getName());
                targetWb = WorkbookFactory.create(new FileInputStream(file));
                Sheet targetSheet = targetWb.getSheet("data");
                guessColByHeadRow(targetSheet.getRow(0));

                for (int i = 1; i <= targetSheet.getLastRowNum(); i++) {
                    processRow(termList, targetSheet.getRow(i));
                }
                LOG.info("successfully Process file {} .", file.getName());
            } catch (Exception e) {
                LOG.error("Failed for processing file " + file.getName(), e);
            } finally {
                IOUtils.closeQuietly(targetWb);
            }
        }

        for (String key : resultMap.keySet()) {
            System.out.println(key + "\t" + Joiner.on(",").join(resultMap.get(key)));
        }

        return true;
    }

    private void initResultMap(List<String> termList) {
        for (String term : termList) {
            resultMap.put(term, Sets.newLinkedHashSet());
        }
    }

    private void guessColByHeadRow(Row headRow) {
        Validate.notNull(headRow, "Invalid format");
        for (int i = 0; i <= headRow.getLastCellNum(); i++) {
            Cell cell = headRow.getCell(i);
            if (cell == null || cell.getCellType() != Cell.CELL_TYPE_STRING) {
                continue;
            }

            String value = cell.getStringCellValue();
            if (value.equalsIgnoreCase("source term")) {
                sourceCol = i;
            } else if (value.equalsIgnoreCase("CertificationQA")) {
                certCol = i;
            } else if (value.equalsIgnoreCase("language")) {
                languageCol = i;
            }
        }
    }

    private void processRow(List<String> termList, Row row) {
        for (String term : termList) {
            String cert = row.getCell(certCol).getStringCellValue();
            String source = row.getCell(sourceCol).getStringCellValue().toLowerCase();
            String language = row.getCell(languageCol).getStringCellValue();
            if (cert.equalsIgnoreCase("2 - Failed") && source.contains(term.toLowerCase())) {
                resultMap.get(term).add(language);
            }
        }
    }
}
