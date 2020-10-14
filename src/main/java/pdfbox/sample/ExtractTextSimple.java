package pdfbox.sample;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.text.PDFTextStripper;

public class ExtractTextSimple {
  public static void processPdf(byte[] input) throws IOException {
    try (PDDocument document = PDDocument.load(input)) {
      AccessPermission ap = document.getCurrentAccessPermission();
      if (!ap.canExtractContent()) {
        throw new IOException("You do not have permission to extract text");
      }

      PDFTextStripper stripper = new PDFTextStripper();

      // This example uses sorting, but in some cases it is more useful to switch it off,
      // e.g. in some files with columns where the PDF content stream respects the
      // column order.
      stripper.setSortByPosition(true);

      for (int p = 1; p <= document.getNumberOfPages(); ++p) {
        // Set the page interval to extract. If you don't, then all pages would be extracted.
        stripper.setStartPage(p);
        stripper.setEndPage(p);

        // let the magic happen
        String text = stripper.getText(document);

        // do some nice output with a header
        String pageStr = String.format("page %d:", p);
        System.out.println(pageStr);
        for (int i = 0; i < pageStr.length(); ++i) {
          System.out.print("-");
        }
        System.out.println();
        System.out.println(text.trim());
        System.out.println();
      }
    }
  }
}
