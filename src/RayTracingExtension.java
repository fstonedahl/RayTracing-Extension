/** See README file for copyright/license information... */

/*
 * TODO: Add more functionality / new primitives
 *  - bump-mapping primitive
 *  - area light sources
 *  - brighten/darken all light sources in the scene?
 *  - spotlights?
 * 
 * TODO: Structural overhaul 
 *    - make it so that agents that share 
 *      features get a prototype shape with all of those common
 *      features in the POV file, so editing that will edit all of them.
 *     "Consider the idea of archetypes -- for each breed (sheep, wolves) or object type (turtles patches), can we create a template object in the POV file that contains all of the attributes (color, size, texture) that the breed has in common? (This would make it easier to manually edit the POV file, for power users.)"
 * 
 * TODO: Get rid of debugging/println statements...
 * 
 * TODO: Improved image mapping - customize "map_type xxx" to image_map, based on shape? 
 * 
 * TODO: Support custom shapes?  Or leave  that for those who tinker with POV source?
 * 
 * TODO: support 3D turtle-trails drawing?  (maybe not worthwhile?)
 * 
 * TODO: Support NetLogo 2D shapes in POVRAY (either flattened like the butterfly, or stand-up images like wolves & sheep?) 
 * 
 * TODO: option for only creating a scene file, without actually running povray
 * 
 * TODO: Extra support for making movies (auto-numbering files)
 * 
 * TODO: Better error handling support.
 * 
 * TODO: Documentation:
 *    - add docs for the clear-all primitive
 *    - Improve docs for installing povray, and configuring the extension.
 *    - explain camera positioning (uses same camera angle as the 3D view)
 *    - explain lighting (uses a headlight by default, otherwise you can add lights using the add-light command)
 *    - need to provide a list of textures they can use, or how to find such a list...
 *         Maybe point to: http://texlib.povray.org/
 */


package org.nlogo.extensions.raytracing;

import org.nlogo.api.Argument;
import org.nlogo.api.Context;
import org.nlogo.api.DefaultClassManager;
import org.nlogo.api.DefaultCommand;
import org.nlogo.api.ExtensionException;
import org.nlogo.api.ExtensionManager;
import org.nlogo.api.LogoException;
import org.nlogo.api.PrimitiveManager;
import org.nlogo.api.Syntax;
import org.nlogo.api.Turtle3D;
import org.nlogo.api.Turtle;
import org.nlogo.api.Patch3D;
import org.nlogo.api.Patch;
import org.nlogo.api.Link;
import org.nlogo.api.Link3D;
import org.nlogo.api.LogoList;

import org.nlogo.api.Agent;
import org.nlogo.api.Perspective;
import java.io.*;

import java.util.*;

import org.nlogo.nvm.ExtensionContext;


public class RayTracingExtension extends DefaultClassManager {
	
	private static String POVRAY_EXE = "povray";
	private static String builtInShapesText = "";
	
    private static ArrayList< ArrayList<Double> > lights = new ArrayList< ArrayList<Double> >();
    private static HashMap<Agent, Double> reflectionMap = new HashMap<Agent, Double>();
    private static HashMap<Agent, Double> refractionMap = new HashMap<Agent, Double>();
    private static HashMap<Agent, Double> highlightMap = new HashMap<Agent, Double>();
    private static HashMap<Agent, Double> highlightSizeMap = new HashMap<Agent, Double>();
    private static HashMap<Agent, String> textureMap = new HashMap<Agent, String>();
    private static HashMap<Agent, Double> textureScalingMap = new HashMap<Agent, Double>();
    private static HashMap<Agent, String> imageMap = new HashMap<Agent, String>();
    private static Integer resolution_width = new Integer(800);
    private static Integer resolution_height = new Integer(600);
    private static Double anti_aliasing = new Double(0.3);
    private static Double background_red = new Double(0.0);
    private static Double background_green = new Double(0.0);
    private static Double background_blue = new Double(0.0);
    private static String background_image = null;

	@Override
	public void clearAll()
	{
		clearAllPOVSettings();
	}
	public static void clearAllPOVSettings()
	{
		lights.clear();
		reflectionMap.clear();
		refractionMap.clear();
		highlightMap.clear();
		highlightSizeMap.clear();
		textureMap.clear();
		textureScalingMap.clear();
		imageMap.clear();
		resolution_width = 800;
		resolution_height = 600;
		anti_aliasing = 0.3;
    	background_red = 0.0;
    	background_green = 0.0;
    	background_blue = 0.0;
		background_image = null;
	}

    public void runOnce(org.nlogo.api.ExtensionManager em) throws ExtensionException
    {
		try {
			builtInShapesText = em.getFile("raytracing/shapes.txt").readFile();
		} catch (IOException ex)
		{
			throw new ExtensionException("Failed to load shapes file: raytracing/shapes.txt");
		}
		try {
			org.nlogo.api.File configFile = em.getFile("raytracing/raytracing.config");
			String wholeFile = configFile.readFile();
			java.util.Properties props = new java.util.Properties();
			props.load(new java.io.StringReader(wholeFile));
			POVRAY_EXE = props.getProperty("povray_executable", "povray2"); 
		} catch (IOException ex)
		{
			throw new ExtensionException("Failed to load config file: raytracing/raytracing.config");
		}
    }	

    @Override
	public void load( PrimitiveManager primitiveManager )
	{
        primitiveManager.addPrimitive( "clear-all", new ClearAll());
		primitiveManager.addPrimitive( "render" , new Render() ) ;
        primitiveManager.addPrimitive( "add-light", new AddLight());
        primitiveManager.addPrimitive( "clear-lights", new ClearLights());
        primitiveManager.addPrimitive( "set-reflection", new SetReflection());
        primitiveManager.addPrimitive( "set-refraction", new SetRefraction());
        primitiveManager.addPrimitive( "set-highlight", new SetHighlight());
        primitiveManager.addPrimitive( "set-texture", new SetTexture());
        primitiveManager.addPrimitive( "set-resolution", new SetResolution());
        primitiveManager.addPrimitive( "set-background", new SetBackground());
        primitiveManager.addPrimitive( "match-window", new MatchWindow());
        primitiveManager.addPrimitive( "anti-aliasing", new SetAntiAliasing());
        primitiveManager.addPrimitive( "set-background-image", new SetBackgroundImage());
        primitiveManager.addPrimitive( "set-image", new SetImage());
	}


    private static double getZcor(Agent a)
    {
        if (org.nlogo.api.Version.is3D())
        {
            if (a instanceof Patch3D)
            {
               return -((Patch3D)a).pzcor();
            }
            else if (a instanceof Turtle3D)
            {
                return -((Turtle3D)a).zcor();
            }
        }
        else
        {
            if (a instanceof Patch)
            {
                return 0;
            }
            else if (a instanceof Turtle)
            {
                return -1;
            }
        }

        throw new IllegalStateException("Unknown agent type!");
    }
    private static double getPitch(Turtle t)
    {
        if (t instanceof Turtle3D)
        {
            return ((Turtle3D)t).pitch();
        }
        else
        {
            return 0;
        }
    }
    private static double getRoll(Turtle t)
    {
        if (t instanceof Turtle3D)
        {
            return ((Turtle3D)t).roll();
        }
        else
        {
            return 0;
        }
    }


    private static double getLinkZ1(Link mylink)
    {
        if (mylink instanceof Link3D)
        {
             return - ((Link3D)mylink).z1();
        }
        else
        {
            return -1;
        }
    }

    private static double getLinkZ2(Link mylink)
    {
        if (mylink instanceof Link3D)
        {
             return - ((Link3D)mylink).z2();
        }
        else
        {
            return -1;
        }
    }

	/** Returns the alpha (transparency) value between 0 (fully transparent) and 255 (fully opaque)
	*/
    private static double getAlpha(Agent a)
    {
		Object col;
		
        if (a instanceof Patch3D)
        {
			col = ((Patch3D) a).pcolor();
			if (col instanceof Double && ((Double) col) == 0.0)
			{
				return 0;  // Special case for black 3D patches, which are assumed transparent
			}
        }
        else if (a instanceof Patch)
        {
			col = ((Patch) a).pcolor();
			return 0.0;
        }
        else if (a instanceof Turtle)
        {
			col = ((Turtle) a).color();
        }
        else if (a instanceof Link)
        {
			col = ((Link) a).color();
        }
		else
		{
        	throw new IllegalStateException("Unknown agent type!");
		}
		if (col instanceof LogoList)
		{
			LogoList lst = (LogoList) col;
			if (lst.size() == 4)
			{
				return ((Double) lst.get(3));
			}
		}
		return 255;
    }

    /**
     *
     * @param agent
     * @return a value between 0 (opaque) and 1 (fully transparent)
     */
    private static double getTransparency(Agent agent) {
        if (agent instanceof Turtle) {
            return (1 - getAlpha((Turtle) agent) / 255.0);
        } else if (agent instanceof Patch3D) {
            return (1 - getAlpha((Patch3D) agent) / 255.0);
        } else if (agent instanceof Patch) {
            return 0;
        } else if (agent instanceof Link) {
            return (1 - getAlpha((Link) agent) / 255.0);
        }
        throw new IllegalStateException("unknown agent type");
    }

    public static class Render extends DefaultCommand
	{
		@Override
		public Syntax getSyntax()
		{
			return Syntax.commandSyntax
			    ( new int[] { Syntax.TYPE_STRING , Syntax.TYPE_NUMBER } ) ;
		}
		@Override
		public String getAgentClassString()
		{
			return "OTPL" ;
		}

        private void addFeatures(Agent agent, StringBuilder sb, double red, double green, double blue, double roll, double pitch, double heading)
        {
            sb.append(String.format("rotate <0, %f, 0>\n", -roll));
            sb.append(String.format("rotate <%f, 0, 0>\n", -pitch));
            sb.append(String.format("rotate <0, 0, %f>\n", -heading));

            if( agent instanceof Turtle )
                {
                    double turtleSize = ((Turtle)agent).size();
                    sb.append(String.format("scale <%f, %f, %f>\n", turtleSize, turtleSize, turtleSize));
                    sb.append(String.format("translate <%f, %f, %f>\n", ((Turtle)agent).xcor(), ((Turtle)agent).ycor(), getZcor(agent)));
                }
            else if( agent instanceof Patch )
                {
                    sb.append(String.format("translate <%d, %d, %f>\n", ((Patch)agent).pxcor(), ((Patch)agent).pycor(), getZcor(agent)));
                }





            if(refractionMap.containsKey(agent))
            {
                sb.append(String.format("interior{ ior %f }\n", refractionMap.get(agent)));
            }

            sb.append("texture {\n");

            if(textureMap.containsKey(agent))
            {
                sb.append(String.format("%s\n", textureMap.get(agent)));
				sb.append(String.format("scale %s", textureScalingMap.get(agent)));
            }
            else
            {
                sb.append("pigment {\n");

                if(imageMap.containsKey(agent))
                {
					String bitmapFmt = (imageMap.get(agent)).substring((imageMap.get(agent)).lastIndexOf('.') + 1);
					if (bitmapFmt.equals("jpg"))
					{
						bitmapFmt = "jpeg";
					}
                    sb.append("image_map {" + bitmapFmt + " \"" + imageMap.get(agent) + "\"" + "\n interpolate 2}\n}");
                }
                else
                {
                    double transp = getTransparency(agent);
                    sb.append(String.format("color rgb <%f, %f, %f> transmit %f \n} \n", red, green, blue, transp));
                }
            }

            sb.append("finish {\n");

            if(reflectionMap.containsKey(agent))
            {
                sb.append(String.format("reflection %f\n", reflectionMap.get(agent)));

            }

            if(highlightMap.containsKey(agent))
            {

                sb.append(String.format("phong %f phong_size %f\n", highlightMap.get(agent), highlightSizeMap.get(agent)));
            }
			sb.append("diffuse 0.6\n");

            sb.append("}\n}\n}\n");
        }

		public void perform( Argument args[] , Context context ) 
				throws ExtensionException, LogoException
		{
			String outputFileName  = args[ 0 ].getString() ;
			int quality  = args[ 1 ].getIntValue() ;
            
			/*We're using the internal workspace and world objects,
			  which are not really part of the public Extensions API,
			  since we'll need to get at some of the innards.
			*/
			ExtensionContext ec = (ExtensionContext) context ;
			org.nlogo.nvm.Workspace workspace = ec.workspace() ;
			org.nlogo.agent.World world = workspace.world() ;


            try
            {
                  /*System.out.printf( "Position of observer: (%f, %f, %f)\n" ,
                        world.observer().oxcor() , world.observer().oycor() , world.observer().ozcor() ) ;
			*/
               // org.nlogo.agent.Protractor3D protractor3D = world3d.protractor3D() ;

                double pitchRadians = StrictMath.toRadians( world.observer().pitch() ) ;
		        double sin = StrictMath.sin( pitchRadians ) ;
		        double distProj = 10.0 * StrictMath.cos( pitchRadians ) ;

		        if( StrictMath.abs( sin ) < org.nlogo.api.World.INFINITESIMAL )
		        {
		        	sin = 0 ;
		        }

                if( StrictMath.abs( distProj ) < org.nlogo.api.World.INFINITESIMAL )
		        {
			        distProj = 0 ;
		        }

		        double headingRadians = StrictMath.toRadians( world.observer().heading() ) ;
		        double cosProj = StrictMath.cos( headingRadians ) ;
		        double sinProj = StrictMath.sin( headingRadians ) ;

		        if( StrictMath.abs( cosProj ) < org.nlogo.api.World.INFINITESIMAL )
		        {
			        cosProj = 0 ;
		        }

                if( StrictMath.abs( sinProj ) < org.nlogo.api.World.INFINITESIMAL )
		        {
			      sinProj = 0 ;
		        }

                double lookatX = world.observer().oxcor() + ( distProj * sinProj );
                double lookatY = world.observer().oycor() + ( distProj * cosProj );
                double lookatZ = world.observer().ozcor() - ( 10.0 * sin );


                double pitchRadians2 = StrictMath.toRadians( world.observer().pitch() + 90 ) ;
		        double sin2 = StrictMath.sin( pitchRadians2 ) ;
		        double distProj2 = 10.0 * StrictMath.cos( pitchRadians2 ) ;

		        if( StrictMath.abs( sin2 ) < org.nlogo.api.World.INFINITESIMAL )
		        {
		        	sin2 = 0 ;
		        }

                if( StrictMath.abs( distProj2 ) < org.nlogo.api.World.INFINITESIMAL )
		        {
			        distProj2 = 0 ;
		        }

		        double headingRadians2 = StrictMath.toRadians( world.observer().heading() ) ;
		        double cosProj2 = StrictMath.cos( headingRadians2 ) ;
		        double sinProj2 = StrictMath.sin( headingRadians2 ) ;

		        if( StrictMath.abs( cosProj2 ) < org.nlogo.api.World.INFINITESIMAL )
		        {
			        cosProj2 = 0 ;
		        }

                if( StrictMath.abs( sinProj2 ) < org.nlogo.api.World.INFINITESIMAL )
		        {
			      sinProj2 = 0 ;
		        }

                double skyX = -( distProj2 * sinProj2 );
                double skyY = -( distProj2 * cosProj2 );
                double skyZ = ( 10.0 * sin2 );


                String filePath;

                String temp2 = workspace.getModelPath();

                if(new java.io.File( outputFileName ).isAbsolute())
                {
                    filePath = outputFileName;
                }
                else
                {
                    if(temp2 == null)
                    {
                      filePath = System.getProperty( "user.home" ) + java.io.File.separator + outputFileName;
                    }
                    else
                    {
                        File temp = new File(temp2);
                        filePath = temp.getParent() + java.io.File.separator + outputFileName;
                    }
                }


                File temp = new File(filePath + ".pov");

                System.out.println( "Created a temp file: " + temp.getAbsolutePath() ) ;

                BufferedWriter out = new BufferedWriter( new FileWriter( temp ) ) ;

                StringBuilder sb = new StringBuilder();
                
                
                sb.append( "#include \"colors.inc\"\n" )
                   .append( "#include \"shapes.inc\"\n" )
                   .append( "#include \"stones.inc\"\n" )
                   .append( "#include \"textures.inc\"\n" )
                   .append( "#include \"woods.inc\"\n")
                   .append( "#include \"glass.inc\"\n")
                   .append( String.format("background {rgb <%f, %f, %f> }\n", background_red, background_green, background_blue))
                   .append( "camera {\n" )
                   .append( String.format("location <%f, %f, %f>\n" ,
                                world.observer().oxcor() , world.observer().oycor() , -(world.observer().ozcor()) ) )
                   .append(String.format("right <%f * 0.835, 0, 0>\n", resolution_width.doubleValue() / resolution_height.doubleValue()))
                   .append(String.format("up <0, 0.835, 0>\n"))
                   .append(String.format("sky <%f, %f, %f>\n",
                                skyX, skyY, -skyZ))
                   .append( String.format("look_at  <%f, %f,  %f>\n" ,
                                lookatX , lookatY ,-lookatZ))
                   .append( "}\n" ) ;

                if (background_image != null && !background_image.equals(""))
                {
					String bitmapFmt = background_image.substring(background_image.lastIndexOf('.') + 1);
					if (bitmapFmt.equals("jpg"))
					{
						bitmapFmt = "jpeg";
					}
                    sb.append("sky_sphere { \n pigment { \n image_map \n {" + bitmapFmt +" \"" + background_image + "\" " + " \n}\n}\n rotate <90,45,0>\n}\n");
                }
                sb.append(builtInShapesText);

                sb.append("\n");
                for( Agent a : world.turtles().agents() )
                {
                    if( a instanceof org.nlogo.api.Turtle )
                    {
                        if( (    ( world.observer().perspective() != Perspective.RIDE )
                              || ( world.observer().targetAgent() != a )
                            ) && ( !( (Turtle) a ).hidden() )
                                && getTransparency((Turtle) a) < 1.0 )
                        {
                            Turtle t = (Turtle) a ;
                             //getOrientation(t);
                            double heading = t.heading();
                            double pitch = getPitch(t) ;
                            double roll = getRoll(t);
                            java.awt.Color turtleColor = org.nlogo.api.Color.getColor(t.color());
                            int turtleColorRGB = turtleColor.getRGB();
                            double blue = (turtleColorRGB & 0xff) / 255.0;
                            double green = ((turtleColorRGB >> 8) & 0xff) / 255.0;
                            double red = ((turtleColorRGB >> 16) & 0xff) / 255.0;

                            double turtleSize = t.size();


                            if((t.shape()).equals("default"))
                            {
                                sb.append("object{MyTurtle\n");
                            }
                            else if((t.shape()).compareTo("sphere") == 0 || (t.shape()).compareTo("circle") == 0)
                            {
                                sb.append("object { MySphere\n");
                            }
                            else if((t.shape()).compareTo("cube") == 0 || (t.shape()).compareTo("square") == 0)
                            {
                                sb.append("object{MyPatch\n");
                            }
                            else if((t.shape()).compareTo("car") == 0)
                            {
                               sb.append("object{myCar\n");
                            }
                            else if((t.shape()).compareTo("cylinder") == 0)
                            {
                                sb.append("object {MyCylinder\n");
                            }
                            else if((t.shape()).compareTo("cone") == 0 || (t.shape()).compareTo("triangle") == 0)
                            {
                                sb.append("object {MyCone\n");
                            }

                            addFeatures(t, sb, red, green, blue, roll, pitch, heading);


                        }
                    }

                }

                for(Agent a : world.patches().agents())
                {
                    if(a instanceof org.nlogo.api.Patch)
                    {
                        Patch p = (Patch) a;
                        if (getTransparency(p) < 1.0)
                        {
                            java.awt.Color patchColor = org.nlogo.api.Color.getColor(p.pcolor());
                            int patchColorRGB = patchColor.getRGB();
                            double blue = (patchColorRGB & 0xff) / 255.0;
                            double green = ((patchColorRGB >> 8) & 0xff) / 255.0;
                            double red = ((patchColorRGB >> 16) & 0xff) / 255.0;

                            sb.append("object{MyPatch\n");
                            //.append(String.format("translate <%d, %d, %d>\n", p.pxcor(), p.pycor(), -(p.pzcor())));

                            addFeatures(p, sb, red, green, blue, 0.0, 0.0, 0.0);
                        }
                    }
                }

                for(Agent a : world.links().agents())
                {
                    Link link = (Link) a ;
                    if (!link.hidden() && getTransparency(link) < 1.0)
                    {
                        java.awt.Color linkColor = org.nlogo.api.Color.getColor(link.color());
                        int linkColorRGB = linkColor.getRGB();
                        double blue = (linkColorRGB & 0xff) / 255.0;
                        double green = ((linkColorRGB >> 8) & 0xff) / 255.0;
                        double red = ((linkColorRGB >> 16) & 0xff) / 255.0;
						double thickness = link.lineThickness();
						if (thickness == 0.0) { 
							thickness = 0.1;
						}

                        sb.append("cylinder{\n")
                        .append(String.format("<%f, %f, %f>,\n", link.x1(), link.y1(), getLinkZ1(link)))
                        .append(String.format("<%f, %f, %f>,\n", link.x2(), link.y2(), getLinkZ2(link)))
                        .append(String.format("%f\n", thickness / 2));

                        addFeatures(link, sb, red, green, blue, 0.0, 0.0, 0.0);
                    }
                }

                if(lights.isEmpty())
                {
                 sb.append(String.format("light_source{ <%f, %f, %f> color rgb <2.0, 2.0, 2.0>}\n", world.observer().oxcor(),
                         world.observer().oycor() , -(world.observer().ozcor())));   
                }
                else
                {
                    for(int i = 0; i < lights.size(); ++i)
                    {
                        ArrayList<Double> tempLight = lights.get(i);

                        sb.append(String.format("light_source{ <%f, %f, %f> color rgb <%f, %f, %f>}\n", tempLight.get(0),
                                            tempLight.get(1), -tempLight.get(2), tempLight.get(3), tempLight.get(4), tempLight.get(5)));
                    }
                }

                String s = sb.toString() ;
                out.write( s ) ;

                out.close() ;

//                ProcessBuilder povrayProcessBuilder = new ProcessBuilder( "C:\\Users\\rumou\\AppData\\Roaming\\POV-Ray\\v3.6\\bin\\pvengine-sse2.exe" ,
//                        "/RENDER" , temp.getAbsolutePath() ,
//                        "/EDIT" , temp.getAbsolutePath() ) ;
                String width_string = "+W";
                String height_string = "+H";
                String quality_string = "+Q";
                width_string += resolution_width;
                height_string += resolution_height;

                ProcessBuilder povrayProcessBuilder;

                if(quality <= 9)
                {
                    quality_string += quality;

                    povrayProcessBuilder = new ProcessBuilder( POVRAY_EXE ,
                        "+I" + temp.getAbsolutePath(), width_string, height_string, quality_string, "+O" + filePath + ".png") ;
                }
                else if(quality == 10)
                {
                    quality_string += 9;

                    povrayProcessBuilder = new ProcessBuilder( POVRAY_EXE ,
                        "+I" + temp.getAbsolutePath(), width_string, height_string, quality_string, "+A" + anti_aliasing, "+O" + filePath + ".png") ;
                }
                else if(quality >= 11)
                {
                    quality_string += 9;

                    povrayProcessBuilder = new ProcessBuilder( POVRAY_EXE ,
							       "+I" + temp.getAbsolutePath(), width_string, height_string, quality_string, "+A" + anti_aliasing, "+J", ((quality==12)?"-D":"+D") , "+O" + filePath + ".png") ;
                }
                else
                {
                    throw new ExtensionException("wrong quality number!");
                }
                povrayProcessBuilder.directory(temp.getParentFile());
                System.out.println(temp.getParent());
                System.out.println(temp.getName());
            //    ProcessBuilder povrayProcessBuilder = new ProcessBuilder( "C:\\Users\\rumou\\AppData\\Roaming\\POV-Ray\\v3.6\\bin\\pvengine-sse2.exe" ,
            //            "+I" + temp.getAbsolutePath(), width_string, height_string, quality_string, "+O" + filePath + ".bmp") ;
                Process process = povrayProcessBuilder.start() ;
                System.out.println( "Successfully launched POV-Ray" ) ;
				process.waitFor();
                System.out.println( "Successfully finished POV-Ray" ) ;

            }
            catch( IOException e )
            {                                 
                // TO DO
                System.out.println( "Got an IOException: " + e.getMessage() ) ;
            }
			catch (InterruptedException ex) { ex.printStackTrace(); }
		}					      
    }


    public static class AddLight extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {Syntax.TYPE_NUMBER, Syntax.TYPE_NUMBER, Syntax.TYPE_NUMBER,
                                                   Syntax.TYPE_READABLE});
        }

        @Override
		public String getAgentClassString()
		{
			return "OTPL" ;
		}

        public void perform( Argument args[] , Context context )
				throws ExtensionException, LogoException
        {
          Double xCor   = args[ 0 ].getDoubleValue() ;
          Double yCor   = args[ 1 ].getDoubleValue() ;
          Double zCor   = args[ 2 ].getDoubleValue() ;
          Object color  = args[ 3 ].get();

          java.awt.Color javaColor = org.nlogo.api.Color.getColor(color);
          int javaColorRGB = javaColor.getRGB();
          double blue = (javaColorRGB & 0xff) / 255.0;
          double green = ((javaColorRGB >> 8) & 0xff) / 255.0;
          double red = ((javaColorRGB >> 16) & 0xff) / 255.0;


          ArrayList<Double> newLight = new ArrayList<Double>();
          newLight.add(xCor);
          newLight.add(yCor);
          newLight.add(zCor);
          newLight.add(red);
          newLight.add(green);
          newLight.add(blue);
          lights.add(newLight);
        }

    }

    public static class ClearLights extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {});
        }

        @Override
		public String getAgentClassString()
		{
			return "OTPL" ;
		}

        public void perform( Argument args[] , Context context )
				throws ExtensionException, LogoException
        {
            lights.clear();
        }
    }

    public static class ClearAll extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {});
        }

        @Override
		public String getAgentClassString()
		{
			return "OTPL" ;
		}

        public void perform( Argument args[] , Context context )
				throws ExtensionException, LogoException
        {
			 clearAllPOVSettings();
        }
    }

    public static class SetRefraction extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {Syntax.TYPE_NUMBER});
        }

        @Override
		public String getAgentClassString()
		{
			return "TPL" ;
		}

        public void perform( Argument args[] , Context context )
				throws ExtensionException, LogoException
        {
            Agent curAgent = context.getAgent();
            Double rate = args[0].getDoubleValue();
            refractionMap.put(curAgent, rate);

        }
    }

    public static class SetReflection extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {Syntax.TYPE_NUMBER});
        }

        @Override
		public String getAgentClassString()
		{
			return "TPL" ;
		}

        public void perform( Argument args[] , Context context )
				throws ExtensionException, LogoException
        {
            Agent curAgent = context.getAgent();
            Double rate = args[0].getDoubleValue();
            reflectionMap.put(curAgent, rate);

        }
    }

    public static class SetHighlight extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {Syntax.TYPE_NUMBER, Syntax.TYPE_NUMBER});
        }

        @Override
		public String getAgentClassString()
		{
			return "TPL" ;
		}

        public void perform( Argument args[] , Context context )
				throws ExtensionException, LogoException
        {
            Agent curAgent = context.getAgent();
            Double highlightAmt = args[0].getDoubleValue();
            Double highlightSize = args[1].getDoubleValue();
            highlightMap.put(curAgent, highlightAmt);
            highlightSizeMap.put(curAgent, highlightSize);

        }
    }

    public static class SetTexture extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[]{Syntax.TYPE_STRING, Syntax.TYPE_NUMBER});
        }

        @Override
        public String getAgentClassString()
        {
            return "TPL";
        }

        public void perform(Argument args[], Context context)
                throws ExtensionException, LogoException
        {
            Agent curAgent = context.getAgent();
            String textureName = args[0].getString();
            Double textureScale = args[1].getDoubleValue();
            textureMap.put(curAgent, textureName);    
            textureScalingMap.put(curAgent,textureScale);        
        }
    }

    public static class SetResolution extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[]{Syntax.TYPE_NUMBER, Syntax.TYPE_NUMBER});
        }

        @Override
        public String getAgentClassString()
        {
            return "O";
        }

        public void perform(Argument args[], Context context)
                throws ExtensionException, LogoException
        {
            resolution_width  = args[0].getIntValue();
            resolution_height = args[1].getIntValue();
        }        
    }

    public static class SetBackground extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[]{Syntax.TYPE_READABLE});
        }

        @Override
        public String getAgentClassString()
        {
            return "O";
        }

        public void perform(Argument args[], Context context)
            throws ExtensionException, LogoException
        {

          Object color  = args[ 0 ].get();

          java.awt.Color javaColor = org.nlogo.api.Color.getColor(color);
          int javaColorRGB = javaColor.getRGB();
          background_blue = (javaColorRGB & 0xff) / 255.0;
          background_green = ((javaColorRGB >> 8) & 0xff) / 255.0;
          background_red = ((javaColorRGB >> 16) & 0xff) / 255.0;
        }
    }

    public static class MatchWindow extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {});
        }

        @Override
        public String getAgentClassString()
        {
            return "O";
        }

        public void perform(Argument args[], Context context)
            throws ExtensionException, LogoException
        {
            ExtensionContext ec = (ExtensionContext) context ;
			org.nlogo.nvm.Workspace workspace = ec.workspace();
            java.awt.image.BufferedImage image = workspace.exportView();
            resolution_width = image.getWidth();
            resolution_height = image.getHeight();
        }
    }

    public static class SetAntiAliasing extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {Syntax.TYPE_NUMBER});

        }

        @Override
        public String getAgentClassString()
        {
            return "O";
        }

        public void perform(Argument args[], Context context)
            throws ExtensionException, LogoException
        {
            anti_aliasing  = args[0].getDoubleValue();

        }
    }

    public static class SetBackgroundImage extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING});
        }

        @Override
        public String getAgentClassString()
        {
            return "O";

        }

        public void perform(Argument args[], Context context)
            throws ExtensionException, LogoException
        {
            background_image = args[0].getString();
        }
    }

    public static class SetImage extends DefaultCommand
    {
        @Override
        public Syntax getSyntax()
        {
            return Syntax.commandSyntax(new int[] {Syntax.TYPE_STRING});
        }

        @Override
        public String getAgentClassString()
        {
            return "TPL";
        }

        public void perform(Argument args[], Context context)
            throws ExtensionException, LogoException
        {
             Agent curAgent = context.getAgent();
             imageMap.put(curAgent, args[0].getString());
        }
    }
}
