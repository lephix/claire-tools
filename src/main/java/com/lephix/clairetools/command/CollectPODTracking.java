package com.lephix.clairetools.command;

import com.google.common.collect.Maps;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

/**
 *
 * Created by longxiang on 16/1/21.
 */
public class CollectPODTracking {
    private static final Logger LOG = LoggerFactory.getLogger(CollectPODTracking.class);

    Environment environment;

    public CollectPODTracking(Environment environment) {
        this.environment = environment;
    }

    public void run() throws Exception {
        String sourceFolderPath = environment.getProperty("source.folder.path");
        String sourcePathPattern = environment.getProperty("source.path.pattern");
        String targetPath = environment.getProperty("target.path");
        Integer targetWeekCol = CellReference.convertColStringToIndex(
                environment.getProperty("target.week.col")
        );
        Integer languageCodeCol = CellReference.convertColStringToIndex(
                environment.getProperty("target.lang.code.col", "B")
        );
        Integer languageCodeRowStart = environment.getProperty("target.lang.code.row.start", Integer.class, 1);

        File folder = new File(sourceFolderPath);
        if (!folder.isDirectory()) {
            LOG.error("source.folder.path={} is not a folder.", sourceFolderPath);
            throw new RuntimeException("Configuration error.");
        }

        FilenameFilter filenameFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.matches(sourcePathPattern);
            }
        };

        // Cache all the language code list.
        Workbook targetWb = WorkbookFactory.create(new FileInputStream(targetPath));
        Sheet targetSheet = targetWb.getSheetAt(0);
        Map<String, Integer> codeMap = buildCodeMap(targetSheet, languageCodeCol, languageCodeRowStart);

        // Iterate all files, collect values, write back to the target workbook.
        for (File file : folder.listFiles(filenameFilter)) {
            try {
                Number value = extractValue(file);
                String sourceLanguageCode = extractLanguageCode(file.getName());
                int rowNum = findTargetRowNum(codeMap, sourceLanguageCode);

                targetSheet.getRow(rowNum).getCell(targetWeekCol).setCellValue(value.doubleValue() * 100);
                LOG.info("Processed file {} successfully.", file.getAbsolutePath());
            } catch (Exception e) {
                LOG.error("Found error {} during processing file {}.", e.getMessage(), file.getAbsolutePath());
            }
        }

        // Close and write back the modified data.
        FileOutputStream fos = new FileOutputStream(targetPath);
        targetWb.write(fos);
        fos.close();
    }

    private Map<String, Integer> buildCodeMap(Sheet sheet, Integer languageCodeCol, int languageCodeRowStart) {
        Map<String, Integer> codeMap = Maps.newHashMap();
        for (int rowNum = languageCodeRowStart; rowNum < sheet.getLastRowNum(); rowNum++) {
            String codeValue = sheet.getRow(rowNum)
                    .getCell(languageCodeCol)
                    .getStringCellValue();
            codeMap.put(codeValue, rowNum);
        }

        return codeMap;
    }

    private Number extractValue(File file) throws IOException, InvalidFormatException {
        Workbook sourceWb = WorkbookFactory.create(file);
        Sheet sheet = sourceWb.getSheetAt(0);
        Number cellValue = sheet.getRow(37).getCell(1).getNumericCellValue();
        sourceWb.close();

        return cellValue;
    }

    private String extractLanguageCode(String fileName) {
        fileName = fileName.toLowerCase();
        Pattern pattern = Pattern.compile(".*ui_(.*)_week.*");
        Matcher matcher = pattern.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new RuntimeException("No language code was extracted from file name = " + fileName);
    }

    private Integer findTargetRowNum(Map<String, Integer> codeMap, String languageCode) {
        for (String targetCode : codeMap.keySet()) {
            if (targetCode.toLowerCase().contains(languageCode)) {
                return codeMap.get(targetCode);
            }
        }

        throw new RuntimeException("No languageCode name matches. languageCode=" + languageCode);
    }

}
