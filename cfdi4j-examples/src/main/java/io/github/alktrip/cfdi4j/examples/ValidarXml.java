package io.github.alktrip.cfdi4j.examples;

import io.github.alktrip.cfdi4j.Cfdi4j;
import io.github.alktrip.cfdi4j.VerificationResult;
import io.github.alktrip.cfdi4j.core.validation.ValidationResult;
import io.github.alktrip.cfdi4j.core.validation.Violation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Checks a CFDI 4.0 file: its seal and certificate, then the XSD and the business rules. Every
 * violation is printed with its code, location and message.
 *
 * <p>Usage: {@code ValidarXml <cfdi.xml>}.
 */
public final class ValidarXml {

    private ValidarXml() {}

    /**
     * Checks a file and prints the result.
     *
     * @param file the CFDI
     * @return whether the file has no errors
     */
    public static boolean run(Path file) {
        byte[] xml;
        try {
            xml = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        VerificationResult sello = Cfdi4j.verify(xml);
        ValidationResult reglas = Cfdi4j.validate(xml);
        sello.result().violations().forEach(ValidarXml::print);
        reglas.violations().forEach(ValidarXml::print);
        boolean valido = sello.isValid() && reglas.isValid();
        System.out.println(file.getFileName() + (valido ? ": válido" : ": con errores"));
        return valido;
    }

    private static void print(Violation v) {
        System.out.println(v.severity() + " " + v.code() + " " + v.location() + ": " + v.message());
    }

    /**
     * Entry point.
     *
     * @param args the file to check
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: ValidarXml <cfdi.xml>");
            System.exit(2);
        }
        System.exit(run(Path.of(args[0])) ? 0 : 1);
    }
}
