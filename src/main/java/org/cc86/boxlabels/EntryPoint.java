package org.cc86.boxlabels;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class EntryPoint
{
    public static final String APPDIR = new File(EntryPoint.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParentFile().getParent();
    
    public static final String CONFIGPATH = APPDIR + File.separator + "config" + File.separator + "config.yml";
    
    public static ConfigFile CONFIG = null;
    static HashMap<String, String> state = null;
    static HashMap<String, String> meta = null;
    
    private static final DumperOptions yamlOptions = new DumperOptions();
    
    static
    {
        yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    }
    
    private static final Logger l = LogManager.getLogger();
    
    public static void main(String[] args)
    {
        Yaml y = new Yaml();
        if (prepareConfigAndLoad())
        {
            if (checkAndInitializeLabelFolder())
            {
                int i=0;
                File[] labels = new File(CONFIG.getLabelfolder()).listFiles();
                for (File label : labels)
                {
                    l.info("processing Label cnt"+i);
                    i++;
                    l.info("Proc:" + label);
                    if ((!label.isFile()) || label.getName().equals("metalist.yml") || label.getName().equals("statefile.yml"))
                    {
                        continue;
                    }
                    if(!label.getName().endsWith("yml"))
                    {
                        continue;
                    }
                    try
                    {
                        processLabel((LabelContent) y.load(new FileReader(label.getPath())));
                    } catch (FileNotFoundException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                Yaml ys = new Yaml(yamlOptions);
                String statefilepath = new File(CONFIG.getLabelfolder(), "statefile.yml").getPath();
                try
                {
                    ys.dump(state, new FileWriter(statefilepath));
                } catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        } else
        {
            l.error("Configuration needs to be checked");
        }
    }
    
    public static void processLabel(LabelContent c)
    {
        if(state.getOrDefault(c.getBoxId()+"","x").equals(c.hashCode()+""))
        {
            return; //skip, no change in the label
        }
        List<String> imagemagickCmd = new LinkedList<>();
        generateImageMagickCommand(c, imagemagickCmd);
        imagemagickCmd.add(CONFIG.getLabelfolder() + File.separator + "pushlabel.jpg");
        
        List<String> curlcmd = new LinkedList<>(); //Steckkarte; Sonstiges, Lüfter
        curlcmd.add("curl");
        curlcmd.add("-F");
        curlcmd.add("dither=0");
        curlcmd.add("-F");
        curlcmd.add("mac=" + meta.get(c.getBoxId()+""));
        curlcmd.add("-F");
        curlcmd.add("file=@" + CONFIG.getLabelfolder() + File.separator + "pushlabel.jpg");
        curlcmd.add("-o");
        curlcmd.add(CONFIG.getLabelfolder() + File.separator + "curlybraces.log");
        curlcmd.add(CONFIG.getEndpoint() + "/imgupload");
        
        try
        {
            runTool(imagemagickCmd.toArray(new String[]{}));
            l.info("imagemagick done");
            runTool(curlcmd.toArray(new String[]{}));
            l.info("curled up");
            try
            {
                Thread.sleep(50000);
            } catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
            state.put(c.getBoxId()+"",c.hashCode()+"");
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
    
    
    // intentionally exposed to reuse for preview making in the UI
    public static void generateImageMagickCommand(LabelContent c, List<String> imagemagickCmd)
    {
        imagemagickCmd.add("convert");
        imagemagickCmd.add("-size");
        imagemagickCmd.add("296x128");
        imagemagickCmd.add("xc:white");
        imagemagickCmd.add("+antialias");
        imagemagickCmd.add("-fill");
        imagemagickCmd.add("red");
        imagemagickCmd.add("-stroke");
        imagemagickCmd.add("red");
        imagemagickCmd.add("-draw");
        imagemagickCmd.add("rectangle 0,0 60,100");
        imagemagickCmd.add("-stroke");
        imagemagickCmd.add("black");
        imagemagickCmd.add("-strokewidth");
        imagemagickCmd.add("3");
        imagemagickCmd.add("-draw");
        imagemagickCmd.add("line 62,0 62,128");
        imagemagickCmd.add("-strokewidth");
        imagemagickCmd.add("1");
        imagemagickCmd.add("-stroke");
        imagemagickCmd.add("white");
        imagemagickCmd.add("-fill");
        imagemagickCmd.add("white");
        imagemagickCmd.add("-font");
        imagemagickCmd.add("Hack-Bold");
        imagemagickCmd.add("-pointsize");
        imagemagickCmd.add("40");
        imagemagickCmd.add("-draw");
        
        String col = c.getColumn();
        col=col.length()>1?col:(" "+col);
        
        imagemagickCmd.add("text 5,40 \"" + col.replace('"', '_') + "\"");
        imagemagickCmd.add("-pointsize");
        imagemagickCmd.add("40");
        imagemagickCmd.add("-draw");
        
        String row = c.getRow()+"";
        row=row.length()>1?row:(" "+row);
        
        imagemagickCmd.add("text 5,75 \"" + row + "\"");
        
        imagemagickCmd.add("-pointsize");
        imagemagickCmd.add("15");
        imagemagickCmd.add("-font");
        imagemagickCmd.add("Hack-Regular");
        imagemagickCmd.add("-draw");
        imagemagickCmd.add("text 3,95 \"" + c.getSection().replace('"', '_') + "\"");
        
        imagemagickCmd.add("-stroke");
        imagemagickCmd.add("black");
        imagemagickCmd.add("-fill");
        imagemagickCmd.add("black");
        imagemagickCmd.add("-pointsize");
        imagemagickCmd.add("15");
        imagemagickCmd.add("-font");
        imagemagickCmd.add("Noto-Sans-Thin");
        imagemagickCmd.add("-draw");
        imagemagickCmd.add("text 5,123 \"Box " + c.getBoxId() + "\"");
        imagemagickCmd.add("-pointsize");
        imagemagickCmd.add("22");
        //LOOP
        int maxline = 5;
        List<String> lines = c.getLines();
        if (maxline > lines.size())
        {
            maxline = lines.size();
        }
        for (int i = 0; i < maxline; i++)
        {
            imagemagickCmd.add("-draw");
            imagemagickCmd.add("text 65," + (24 * (i + 1)) + " \"* " + lines.get(i).replace('"', '_') + "\"");
        }
        
        imagemagickCmd.add("-quality");
        imagemagickCmd.add("100%");
        imagemagickCmd.add("+dither");
        imagemagickCmd.add("-posterize");
        imagemagickCmd.add("2");
    }
    
    public static boolean checkAndInitializeLabelFolder()
    {
        Yaml y = new Yaml(yamlOptions);
        File labelfolder = new File(CONFIG.getLabelfolder());
        File labelstate = new File(labelfolder, "statefile.yml");
        if (labelfolder.exists())
        {
            if (labelstate.exists())
            {
                try
                {
                    state = (HashMap<String, String>) y.load(new FileReader(labelstate.getPath()));
                    meta = (HashMap<String, String>) y.load(new FileReader(new File(labelfolder, "metalist.yml")));
                } catch (FileNotFoundException e)
                {
                    l.error("How the hell? file disappeared while loading");
                    return false;
                }
                return true;
            }
        }
        
        l.info("preparing initial folder");
        
        
        state = new HashMap<>();
        state.put("42", "0");
        meta = new HashMap<>();
        meta.put("42", "0000000000000000");
        
        LabelContent samplelabel = new LabelContent();
        String samplelabelpath = new File(labelfolder, "samplelabel.yml").getPath();
        String labelmetapath = new File(labelfolder, "metalist.yml").getPath();
        String statefilepath = new File(labelfolder, "statefile.yml").getPath();
        samplelabel.setColumn("A");
        samplelabel.setBoxId(42);
        samplelabel.setRow(38);
        List<String> lines = new ArrayList<>();
        lines.add("NullPoinZer");
        lines.add("AMD-Prozessoren");
        lines.add("Sicherungen");
        lines.add("Satanskabel");
        lines.add("Jalapengs");


        samplelabel.setLines(lines);
        try
        {
            labelfolder.mkdirs();
            y.dump(samplelabel, new FileWriter(samplelabelpath));
            y.dump(meta, new FileWriter(labelmetapath));
            y.dump(state, new FileWriter(statefilepath));
        } catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return false;
    }
    
    public static boolean prepareConfigAndLoad()
    {
        Yaml y = new Yaml(yamlOptions);
        if (new File(CONFIGPATH).exists())
        {
            try
            {
                CONFIG = (ConfigFile) y.load(new FileReader(CONFIGPATH));
            } catch (FileNotFoundException ex)
            {
                ex.printStackTrace();
                return false;
            }
            return true;
        } else
        {
            l.info("Initial config file created, please configure...");
            CONFIG = new ConfigFile();
            CONFIG.setEndpoint("127.0.0.1");
            CONFIG.setLabelfolder("/path/to/labelfolder");
            try
            {
                new File(CONFIGPATH).getParentFile().mkdirs();
                y.dump(CONFIG, new FileWriter(CONFIGPATH));
            } catch (IOException ex)
            {
                ex.printStackTrace();
            }
            LabelContent samplelabel = new LabelContent();
            return false;
        }
    }
    
    public static void runTool(String... args) throws IOException
    {
        
        runTool(null, args);
    }
    
    
    public static void runTool(File workdir, String... args) throws IOException
    {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command(args);
        if (workdir != null && workdir.isDirectory())
        {
            pb.directory(workdir);
        }
        pb.inheritIO();
        Process runme = pb.start();
        try
        {
            runme.waitFor();
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    
}
