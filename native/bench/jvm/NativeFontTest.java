package com.lx862.jcm.nativeapi;

import com.lx862.mtrscripting.core.util.ScriptResourceUtil;
import org.mtr.mapping.holder.Identifier;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.vehicle.VehicleWrapper;
import org.mtr.core.data.SimplifiedRoute;
import org.mtr.core.data.Station;
import org.mtr.mod.data.VehicleExtension;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import javax.imageio.ImageIO;

/** Independent Java2D references exercise actual font files and the JNI output
 * of WR2-A03, rather than checking only that some glyph pixels exist. */
final class NativeFontTest {
    static final String HAN = "mtr:wr2a03/fonts/source-han-sans/source-han-sans-medium.otf";
    static final String LAO = "mtr:wr2a03/fonts/sans_lao/sans_lao.ttf";
    private static int checks;
    private static byte[] bytes(String s) { return s.getBytes(StandardCharsets.UTF_8); }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    private static Font font(String path, double size) {
        return ScriptResourceUtil.readFont(new Identifier(path)).deriveFont(Font.PLAIN, (float) size);
    }
    private static void hints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }
    private static BufferedImage reference(String path, String text, double size, double sx, double sy,
                                            double compression, double x, double y, int width, int height,
                                            Rectangle clip) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var g = image.createGraphics();
        hints(g);
        g.setClip(clip);
        g.scale(sx, sy);
        g.translate(x, y);
        g.scale(compression, 1);
        g.setFont(font(path, size));
        g.setColor(Color.WHITE);
        g.drawString(text, 0f, 0f);
        g.dispose();
        return image;
    }
    static void run() throws Exception {
        String[] paths={HAN, "mtr:wr2a03/fonts/source-han-sans/source-han-sans-regular.otf",
            "mtr:wr2a03/fonts/arial/arial.ttf", "mtr:wr2a03/fonts/arial/arialbd.ttf", LAO,
            "mtr:wr2a03/fonts/honglei/hongleixingshu.otf"};
        var sheet=new BufferedImage(1100,paths.length*130,BufferedImage.TYPE_INT_ARGB);
        var sheetG=sheet.createGraphics(); sheetG.setColor(Color.WHITE); sheetG.fillRect(0,0,1100,sheet.getHeight());
        for(int i=0;i<paths.length;i++) {
            String path=paths[i];
            String text=i<2 || i==5 ? "晏阳中心 → 下一站" : "WR2-A03 10010 01 gypQ";
            final double size=60;
            for(double[] scale:new double[][]{{1,1,1},{1.18,1.18,1},{1.18,1.18,0.65},{1.4,0.9,0.8}}) {
                double sx=scale[0],sy=scale[1],compression=scale[2],x=18.75,y=100.25;
                var g=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB).createGraphics();
                hints(g); g.scale(sx,sy); var fm=g.getFontMetrics(font(path,size));
                double[] actual=NativeHost.get().measureFontText(bytes(path),bytes(text),size,sx,sy);
                check(Arrays.equals(actual,new double[]{fm.stringWidth(text),fm.getAscent(),fm.getDescent(),fm.getHeight()}),
                    "resource FontMetrics must match Java2D: "+path);
                g.dispose();
                for(Rectangle clip:new Rectangle[]{new Rectangle(0,0,1100,160),new Rectangle(30,48,240,58)}) {
                    var expected=reference(path,text,size,sx,sy,compression,x,y,1100,160,clip);
                    var buffer=ByteBuffer.allocateDirect(1100*160*4).order(ByteOrder.nativeOrder());
                    int[] bounds=NativeHost.get().drawFontText(bytes(path),bytes(text),size,x,y,sx,sy,compression,
                        0xffffffff,clip.x,clip.y,clip.x+clip.width,clip.y+clip.height,buffer,1100,160);
                    check(bounds!=null,"resource text draw succeeds: "+path);
                    int[] pixels=((DataBufferInt)expected.getRaster().getDataBuffer()).getData();
                    int differences=0,maxDiff=0;
                    for(int p=0;p<pixels.length;p++) {
                        int delta=Math.abs((buffer.getInt(p*4)>>>24)-(pixels[p]>>>24));
                        if(delta>0) differences++;
                        maxDiff=Math.max(delta,maxDiff);
                    }
                    check(maxDiff<=1,"Java2D glyph shape/baseline/clip mismatch: "+path+", pixels="+differences+", max="+maxDiff);
                    // Repeat through the mask cache.
                    var again=ByteBuffer.allocateDirect(1100*160*4).order(ByteOrder.nativeOrder());
                    NativeHost.get().drawFontText(bytes(path),bytes(text),size,x,y,sx,sy,compression,
                        0xffffffff,clip.x,clip.y,clip.x+clip.width,clip.y+clip.height,again,1100,160);
                    check(buffer.equals(again),"cached masks must reproduce the same pixels");
                }
            }
            var sample=reference(path,text,60,1,1,1,24,100,1100,130,new Rectangle(0,0,1100,130));
            var sg=sample.createGraphics(); sg.setComposite(AlphaComposite.SrcIn); sg.setColor(Color.BLACK);
            sg.fillRect(0,0,1100,130); sg.dispose(); sheetG.drawImage(sample,0,i*130,null);
        }
        sheetG.dispose();
        Path output=Path.of(System.getProperty("jcm.test.output"));
        ImageIO.write(sheet,"png",output.resolve("resource-fonts.png").toFile());
        System.out.println("Native resource font reference checks: "+checks);
    }
    static void plate(BufferedImage actual, String text, int size, String name) throws Exception {
        var expected=reference(LAO,text,size,1,1,1,0,228,1120,240,new Rectangle(0,0,1120,240));
        var g=expected.createGraphics(); g.setComposite(AlphaComposite.Clear); g.fillRect(0,0,1120,240);
        g.setComposite(AlphaComposite.SrcOver); hints(g); g.setFont(font(LAO,size));
        g.setColor(Color.BLACK);
        g.drawString(text,(1120-g.getFontMetrics().stringWidth(text))/2f,228f); g.dispose();
        int[] want=((DataBufferInt)expected.getRaster().getDataBuffer()).getData();
        int[] got=((DataBufferInt)actual.getRaster().getDataBuffer()).getData();
        check(Arrays.equals(want,got),"C++/JNI "+name+" must match the JavaScript Sans Lao plate pixel for pixel");
        ImageIO.write(actual,"png",Path.of(System.getProperty("jcm.test.output"),name+".png").toFile());
        System.out.println("Verified native "+name+" equals Java2D Sans Lao reference");
    }

    static void previews() throws Exception {
        if (System.getProperty("jcm.test.pack") == null) return;
        var wrapper = new VehicleWrapper(VehicleScriptContext.DataFetchMode.ALL, new VehicleExtension());
        wrapper.count = 1;
        wrapper.siding = "10010/01";
        String[] names = {"晏阳中心|Yanyang Central", "人民广场|People's Square", "轨道科技园|Railway Technology Park",
                "临江大道|Riverside Avenue", "若益宛|Ruoyiwan", "南湖|South Lake"};
        for (int i = 0; i < names.length; i++) {
            var stop = new VehicleWrapper.Stop();
            stop.name = names[i];
            stop.destinationName = names[names.length - 1];
            stop.route = new SimplifiedRoute(90, "1号线|Line 1");
            final long stationId = 100 + i;
            stop.station = new Station() { public long getId() { return stationId; } };
            var transfer = new VehicleWrapper.Stop.RouteInterchange();
            transfer.name = "3号线|Line 3";
            stop.routeInterchanges.add(transfer);
            wrapper.all.add(stop);
            wrapper.current.add(stop);
        }
        var snapshot = NativeSnapshot.build(ByteBuffer.allocateDirect(NativeSnapshot.CAPACITY), wrapper);
        var time = NativeSnapshot.class.getDeclaredField("OFF_GAME_TIME"); time.setAccessible(true);
        var door = NativeSnapshot.class.getDeclaredField("OFF_DOOR_VALUE"); door.setAccessible(true);
        final int timeOffset = time.getInt(null), doorOffset = door.getInt(null);
        final long startTime = snapshot.getLong(timeOffset);
        for (int mode = 0; mode < 3; mode++) {
            snapshot.putLong(timeOffset, startTime + mode * 11000);
            if (mode == 2) snapshot.putDouble(doorOffset, 1.0);
            var frames = NativeScriptManager.renderVehicle("font-preview/wr2a03", "wr2a03", snapshot);
            check(frames.size() == 2, "both native modules produce preview frames");
            var managers = NativeDrawRegistry.begin("font-preview", "wr2a03", 1);
            var apply = NativeVehicleDriver.class.getDeclaredMethod("applyFrame",
                    NativeScriptManager.NativeFrame.class, java.util.Map.class);
            apply.setAccessible(true);
            for (var frame : frames) check((boolean) apply.invoke(null, frame, managers), "preview frame is applied");
            String owner = frames.get(0).instanceKey;
            var texture = NativeHost.get().texture(0, owner);
            check(texture != null, "preview LCD receives uploaded native pixels");
            check((texture.bufferedImage.getRGB(0, 0) >>> 24) == 255, "preview LCD is painted, not an empty allocation");
            String name = new String[]{"lcd-full", "lcd-partial", "lcd-doors-open"}[mode];
            ImageIO.write(texture.bufferedImage, "png",
                    Path.of(System.getProperty("jcm.test.output"), name + ".png").toFile());
        }
    }
}
