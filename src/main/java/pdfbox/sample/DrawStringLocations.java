package pdfbox.sample;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/**
 * This is an example on how to get some x/y coordinates of text and to show them in a rendered
 * image.
 *
 * @author Ben Litchfield
 * @author Tilman Hausherr
 */
// https://stackoverflow.com/questions/21430341/identifying-the-text-based-on-the-output-in-pdf-using-pdfbox
// https://stackoverflow.com/questions/20878170/how-to-determine-artificial-bold-style-artificial-italic-style-and-artificial-o/20924898#20924898
public class DrawStringLocations extends PDFTextStripper {
  private AffineTransform flipAT;
  private AffineTransform rotateAT;
  private AffineTransform transAT;
  private final String filename;
  static final int SCALE = 4;
  private Graphics2D g2d;

  public DrawStringLocations(PDDocument document, String filename) throws IOException {
    this.document = document; // must initialize here, base class initializes too late
    this.filename = filename;
  }

  /**
   * This will print the documents data.
   *
   * @throws IOException If there is an error parsing the document.
   */
  public static void processPdf(String filename) throws IOException {

    try (PDDocument document = PDDocument.load(new File(filename))) {
      DrawStringLocations stripper = new DrawStringLocations(document, filename);
      stripper.setSortByPosition(true);

      for (int page = 0; page < document.getNumberOfPages(); ++page) {
        stripper.stripPage(page);
      }
    }
  }

  private void stripPage(int page) throws IOException {
    PDFRenderer pdfRenderer = new PDFRenderer(document);
    BufferedImage image = pdfRenderer.renderImage(page, SCALE);
    PDPage pdPage = document.getPage(page);
    PDRectangle cropBox = pdPage.getCropBox();

    // flip y-axis
    flipAT = new AffineTransform();
    flipAT.translate(0, pdPage.getBBox().getHeight());
    flipAT.scale(1, -1);

    // page may be rotated
    rotateAT = new AffineTransform();
    int rotation = pdPage.getRotation();
    if (rotation != 0) {
      PDRectangle mediaBox = pdPage.getMediaBox();
      switch (rotation) {
        case 90:
          rotateAT.translate(mediaBox.getHeight(), 0);
          break;
        case 270:
          rotateAT.translate(0, mediaBox.getWidth());
          break;
        case 180:
          rotateAT.translate(mediaBox.getWidth(), mediaBox.getHeight());
          break;
        default:
          break;
      }
      rotateAT.rotate(Math.toRadians(rotation));
    }

    // cropbox
    transAT =
        AffineTransform.getTranslateInstance(-cropBox.getLowerLeftX(), cropBox.getLowerLeftY());

    g2d = image.createGraphics();
    g2d.setStroke(new BasicStroke(0.1f));
    g2d.scale(SCALE, SCALE);

    setStartPage(page + 1);
    setEndPage(page + 1);

    Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
    writeText(document, dummy);

    g2d.dispose();

    String imageFilename = filename;
    int pt = imageFilename.lastIndexOf('.');
    imageFilename = imageFilename.substring(0, pt) + "-strings-" + (page + 1) + ".png";
    ImageIO.write(image, "png", new File(imageFilename));
  }

  /** Override the default functionality of PDFTextStripper. */
  @Override
  protected void writeString(String string, List<TextPosition> textPositions) {
    List<Rectangle2D> bounds =
        textPositions.stream()
            .map(this::getBounds)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    if (bounds.size() > 0) {
      Rectangle2D bound = union(bounds);
      g2d.setColor(Color.blue);
      g2d.draw(bound);
    }
  }

  private static Rectangle2D union(List<Rectangle2D> bounds) {
    Rectangle2D result = bounds.get(0);
    for (int i = 1; i < bounds.size(); ++i) {
      result = result.createUnion(bounds.get(i));
    }
    return result;
  }

  private Rectangle2D getBounds(TextPosition text) {
    try {
      AffineTransform at = text.getTextMatrix().createAffineTransform();

      PDFont font = text.getFont();
      BoundingBox bbox = font.getBoundingBox();

      // advance width, bbox height (glyph space)
      float xadvance = font.getWidth(text.getCharacterCodes()[0]); // todo: should iterate all chars
      Rectangle2D.Float rect =
          new Rectangle2D.Float(0, bbox.getLowerLeftY(), xadvance, bbox.getHeight());

      if (font instanceof PDType3Font) {
        // bbox and font matrix are unscaled
        at.concatenate(font.getFontMatrix().createAffineTransform());
      } else {
        // bbox and font matrix are already scaled to 1000
        at.scale(1 / 1000f, 1 / 1000f);
      }
      Shape s = at.createTransformedShape(rect);
      s = flipAT.createTransformedShape(s);
      s = rotateAT.createTransformedShape(s);

      return s.getBounds2D();
    } catch (IOException e) {
      return null;
    }
  }
}
