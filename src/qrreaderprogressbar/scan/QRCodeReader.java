/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package qrreaderprogressbar.scan;


import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;

import com.google.zxing.common.HybridBinarizer;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;


//import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Properties;
import org.imgscalr.Scalr;
import net.sf.javavp8decoder.imageio.*;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
//import net.coobird.thumbnailator.Thumbnails;
//import net.coobird.thumbnailator.resizers.configurations.ScalingMode;





/**
 *
 * @author Artem.Kovalev
 */
public class QRCodeReader {
    
    static Properties prop = new Properties();
    
    public final static float STEP = 0.1f;
    public final static int MINSIZE = 300;
    public final static int MAXSIZE32bit=4500;
    
    private static float cStep = STEP;
    private static int cMinsize = MINSIZE;
    
    private static File logfile = new File("log.log");
    private static PrintWriter logwriter=null;
    
    private static boolean b64 = false;
    
    private static Scalr.Method method = Scalr.Method.AUTOMATIC;

    public static float getcStep() {
        return cStep;
    }

    public static int getcMinsize() {
        return cMinsize;
    }

    public static File getLogfile() {
        return logfile;
    }

    public static boolean isB64() {
        return b64;
    }

    public static Scalr.Method getMethod() {
        return method;
    }
    
    
    
    public static void readProperties() 
    {
        InputStream fis=null;
        try {
            fis = new FileInputStream("settings.properties");
            prop.load(fis);
            
            String step = prop.getProperty("step");
            String minsize = prop.getProperty("minsize");
            
            if (step!=null && !step.isEmpty())
            {
                Float st = Float.valueOf(step);
                if (st!=null && st>0 && st<=0.5)
                    cStep=st;
            }
            
            if (minsize!=null && !minsize.isEmpty())
            {
                Integer ms = Integer.valueOf(minsize);
                if (ms!=null && ms>0)
                    cMinsize=ms;
            }
            if (System.getProperty("java.vm.name").contains("64-Bit"))
                b64=true;
            String logval = prop.getProperty("log","false");
            if (logval.equalsIgnoreCase("true") || logval.equalsIgnoreCase("1"))
                try {
                    logwriter = new PrintWriter(logfile);
                    logwriter.println(System.getProperty("java.vm.name"));
                } catch (FileNotFoundException ex) {
                    System.out.println("Cannot open log");
                }
            String methodprop = prop.getProperty("resize","AUTOMATIC");
            
            //System.out.println(Arrays.toString(Scalr.Method.values()));
            List<String> lstr = Arrays.asList(new String[]{"AUTOMATIC","BALANCED","QUALITY","SPEED","ULTRA_QUALITY"});
            
            if (lstr.contains(methodprop))  
                method=Scalr.Method.valueOf(methodprop);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(QRCodeReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(QRCodeReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally
        {
            try {
                if (fis!=null)
                    fis.close();
            } catch (IOException ex) {
               
            }
        }
    }
    
    
    public static void closeLog()
    {
        logfile=null;
        if (logwriter!=null)
            logwriter.close();
       
    }
    private static String decodeQRCode(File qrCodeimage)
    {
        
        boolean dolog=!(logwriter==null);
        
        if (dolog) logwriter.println("file:"+qrCodeimage.getName());
        System.out.println("file:"+qrCodeimage.getName());
        try 
        {
            BufferedImage bufferedImage = null;
            
            try
            {
                if (qrCodeimage.getName().endsWith(".webp"))
                {

                    ImageReader ir = new WebPImageReader(new WebPImageReaderSpi());
                    ir.setInput(ImageIO.createImageInputStream(qrCodeimage));
                    bufferedImage = ir.read(1);
                }
                else
                {
                    bufferedImage = ImageIO.read(qrCodeimage);
                }
            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
                if (logwriter!=null) logwriter.println(e.getMessage());
                return ("Cannot red image");
            }
            if (bufferedImage==null)
                throw new IOException();
            int bimaxsize=Math.max(bufferedImage.getHeight(),bufferedImage.getWidth());
            if (bimaxsize>MAXSIZE32bit && !b64)
            {
                try
                {
                    
                    bufferedImage = Scalr.resize(bufferedImage, method, MAXSIZE32bit);
                    //if (logwriter!=null) logwriter.println("Scalr made resize.");
                    
                    //bufferedImage = Thumbnails.of(bufferedImage).scalingMode(ScalingMode.PROGRESSIVE_BILINEAR).scale(getRatio(bufferedImage,MAXSIZE32bit))
                    //    .asBufferedImage();
                    
                    
                    if (logwriter!=null) logwriter.println("Scalr made resize.");
                    //bufferedImage=createResizedCopy(bufferedImage,MAXSIZE32bit,false);
                }
                catch (Exception e)
                {
                    if (dolog) logwriter.println(e.getMessage());
                    return "Cannot resize picture";
                }
                    //bufferedImage=Scalr.resize(bufferedImage, Scalr.Method.SPEED, Scalr.Mode.FIT_EXACT, bimaxsize,Scalr.OP_GRAYSCALE);
                      
                        //Scalr.resize(bufferedImage, Scalr.Mode.FIT_EXACT, 4500);
                if (dolog) logwriter.println("image resized to fit 4500pxl box");
            }
            
            String res="";
            boolean stop=false;
            MultiFormatReader decoder = new MultiFormatReader();
            int iter=0;
            while (!stop)
            {
                int maxsize=Math.max(bufferedImage.getHeight(),bufferedImage.getWidth());
                int cursize=Math.round(maxsize*(1.0f-iter*cStep));
                
                if (cursize<cMinsize)
                {
                    if (dolog) logwriter.println("\t\t There is no QR code in the image");
                    return ("There is no QR code in the image");
                }
                if (dolog) logwriter.println("\t "+cursize+ ":");
                System.out.println("\t "+cursize+ ":");
                BufferedImage curImage=null;
                if (iter==0)
                    curImage=bufferedImage;
                else
                    //curImage = Scalr.resize(bufferedImage,Scalr.Method.ULTRA_QUALITY,cursize);
                    curImage = Scalr.resize(bufferedImage,method,cursize);
                if (curImage==null)
                {
                    if (dolog) logwriter.println("Cannot read image");
                    return "Cannot read image";
                }
                res=decodeQR(curImage,decoder);
                iter++;
                if (res!=null)
                    stop=true;
            }
            decoder.reset();
            if (dolog) logwriter.println("\t\t "+res);
            return res;
        } 
        catch (IOException ex) {
            if (dolog) logwriter.println("Problem with file");
            return ("Problem with file");
        }
    }
    
    private static float getRatio(BufferedImage originalImage, int boxSize)
    {
        int w = originalImage.getWidth();
        int h = originalImage.getHeight();
        if (logwriter!=null) logwriter.println("originalWidth="+w+" OriginalHeight="+h);
        float ratio=1.0f;
        if (h>w)
            ratio = (float)boxSize/h;
        else
            ratio = (float)boxSize/w;
        return ratio;
    }
            
    
    static BufferedImage createResizedCopy(BufferedImage originalImage, 
            int boxSize, 
            boolean preserveAlpha)
    {
        int w = originalImage.getWidth();
        int h = originalImage.getHeight();
        if (logwriter!=null) logwriter.println("originalWidth="+w+" OriginalHeight="+h);
        float ratio=1.0f;
        if (h>w)
            ratio = (float)boxSize/h;
        else
            ratio = (float)boxSize/w;
        if (logwriter!=null) logwriter.println("Ratio="+ratio);
        
       
        int scaledWidth = Math.round(w*ratio);
        int scaledHeight = Math.round(h*ratio);
        
        int imageType = preserveAlpha ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, imageType);
        Graphics2D g = scaledBI.createGraphics();
        if (preserveAlpha) {
            g.setComposite(AlphaComposite.Src);
        }
        g.drawImage(originalImage, 0, 0, scaledWidth, scaledHeight, null); 
        g.dispose();
        return scaledBI;
    }
    private static String decodeQR (BufferedImage bi, MultiFormatReader decoder)
    {
        LuminanceSource source = new BufferedImageLuminanceSource(bi);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Map<DecodeHintType,Object> tmpHintsMap = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        tmpHintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        tmpHintsMap.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));   
        try
        {
            Result result = decoder.decode(bitmap,tmpHintsMap);
            if (result!=null)
                    System.out.println("\t\t"+result.getBarcodeFormat().name()+"\t"+result.getText());
            return result.getText();
        }
        catch (NotFoundException e) 
        {
            return null;
        }
    }
    
    public static String scan(File qrCodeimage)
    {
        return decodeQRCode(qrCodeimage);
    }
    
    /*
    private static String decodeQRCode (BufferedImage bisource, int iter) 
    {
        int maxsize=Math.max(bisource.getHeight(),bisource.getWidth());
        int cursize=Math.round(maxsize*(1.0f-iter*cStep));
        
        //BufferedImage bi = Scalr.resize(bisource, cursize);
        
        BufferedImage bi;
        try {
            bi = scale(bisource,1.0f-iter*cStep);
        } catch (IOException ex) {
            return "Error Resizing Picture";
        }
                
                
        LuminanceSource source = new BufferedImageLuminanceSource(bi);
            
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            
        Map<DecodeHintType,Object> tmpHintsMap = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        
        tmpHintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        
        tmpHintsMap.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
        
        //tmpHintsMap.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
        
        MultiFormatReader decoder = new MultiFormatReader();
        
        try
        {
            System.out.println("\t "+cursize+ ":");
            Result result = decoder.decode(bitmap,tmpHintsMap);
            if (result!=null)
                    System.out.println("\t\t"+result.getBarcodeFormat().name()+"\t"+result.getText());
            return result.getText();
        }
        catch (NotFoundException e) {
            int nextsize=Math.round(maxsize*(1.0f-(iter+1)*cStep));
            if (nextsize>=cMinsize)
            {
                return decodeQRCode(bisource,++iter);
            }
            else
            {
                System.out.println("\t\t"+"There is no QR code in the image");
                return ("There is no QR code in the image");
            }
        }
        finally
        {
            decoder=null;
        }
    }
    
    
    
    private static String decodeQRCode (BufferedImage bi) 
    {
        LuminanceSource source = new BufferedImageLuminanceSource(bi);
            
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            
        Map<DecodeHintType,Object> tmpHintsMap = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
        
        tmpHintsMap.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        
        tmpHintsMap.put(DecodeHintType.POSSIBLE_FORMATS, EnumSet.of(BarcodeFormat.QR_CODE));
        
        //tmpHintsMap.put(DecodeHintType.PURE_BARCODE, Boolean.TRUE);
        
        int maxsize=Math.max(bi.getHeight(),bi.getWidth());
        
        MultiFormatReader decoder = new MultiFormatReader();
        
        try
        {
            System.out.println("\t "+maxsize+ ":");
            Result result = decoder.decode(bitmap,tmpHintsMap);
            if (result!=null)
                    System.out.println("\t\t"+result.getBarcodeFormat().name()+"\t"+result.getText());
            return result.getText();
        }
        catch (NotFoundException e) {
            if (maxsize>=cMinsize)
            {
                
                BufferedImage scaledImage = Scalr.resize(bi, Math.round(maxsize*(1.0f-cStep)));
                return decodeQRCode(scaledImage);
            }
            else
            {
                System.out.println("\t\t"+"There is no QR code in the image");
                
                return ("There is no QR code in the image");
            }
        }
        finally
        {
            decoder=null;
        }
    }
    
    public static BufferedImage resize(BufferedImage img, int newW, int newH) throws IOException {
        return Thumbnails.of(img).size(newW, newH).asBufferedImage();
    }
    
    public static BufferedImage scale(BufferedImage img, double scale) throws IOException {
        return Thumbnails.of(img).
                scale(scale).asBufferedImage();
    }
    
    private static BufferedImage toBufferedImage(Image img)
    {
        if (img instanceof BufferedImage)
        {
            return (BufferedImage) img;
        }

        // Create a buffered image with transparency
        BufferedImage bimage = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_BYTE_GRAY);
        

        // Draw the image on to the buffered image
        Graphics2D bGr = bimage.createGraphics();
        bGr.drawImage(img, 0, 0, null);
        bGr.dispose();

        // Return the buffered image
        return bimage;
    }
    
    private static String BfCVdecode (File f)
    {
        BufferedImage i = UtilImageIO.loadImage(f.getAbsolutePath());
        return BfCVdecode(i);
    }
    
    private static String BfCVdecode (BufferedImage bi) 
    {
        
        GrayU8 gray = ConvertBufferedImage.convertFrom(bi,(GrayU8)null);

	QrCodeDetector<GrayU8> detector = FactoryFiducial.qrcode(null,GrayU8.class);

	detector.process(gray);
        
        List<QrCode> detections = detector.getDetections();
        
        for( QrCode qr : detections ) {
            // The message encoded in the marker
            System.out.println("BoofCV detection message: "+qr.message);
            
            // Visualize its location in the image
            //VisualizeShapes.drawPolygon(qr.bounds,true,1,g2);
        }
        
        List<QrCode> failures = detector.getFailures();
	
	for( QrCode qr : failures ) 
        {
            System.out.println("BoofCV failures message: "+qr.message);
            // If the 'cause' is ERROR_CORRECTION or later then it's probably a real QR Code that
            if( qr.failureCause.ordinal() < QrCode.Failure.ERROR_CORRECTION.ordinal() )
		continue;
            
		//VisualizeShapes.drawPolygon(qr.bounds,true,1,g2);
	}
        
        return "detections: "+detections.size()+" failures:"+failures.size();
    }*/
}
