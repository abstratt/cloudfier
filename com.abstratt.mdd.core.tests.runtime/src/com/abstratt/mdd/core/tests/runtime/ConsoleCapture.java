package com.abstratt.mdd.core.tests.runtime;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Utility for capturing the output produced by a snippet of code.
 */
public class ConsoleCapture {

    /**
     * Returns a list containing all lines sent to the standard output while
     * running the given runnable as its elements.
     */
    public List<String> collect(Runnable runnable) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(baos);
        PrintStream savedOutput = System.out;
        System.setOut(output);
        try {
            runnable.run();
        } finally {
            System.setOut(savedOutput);
        }
        output.close();
        String text = new String(baos.toByteArray());
        StringTokenizer tokenizer = new StringTokenizer(text, System.getProperty("line.separator"));
        List<String> result = new ArrayList<String>();
        while (tokenizer.hasMoreTokens())
            result.add(tokenizer.nextToken());
        return result;
    }
}
