package zombie.javamods;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import se.krka.kahlua.converter.KahluaConverterManager;
import se.krka.kahlua.j2se.J2SEPlatform;
import se.krka.kahlua.vm.KahluaTable;
import se.krka.kahlua.vm.Platform;

import zombie.gameStates.MainScreenState;
import zombie.Lua.LuaManager;


public class MainJavaMods implements Runnable {

	private MainJavaMods() {
		new Thread(this).start();
	}

	private void exposeJavaMods(File javaModsDir) throws Exception {

		Field managerField = LuaManager.class.getDeclaredField("converterManager");
		KahluaConverterManager manager = (KahluaConverterManager)managerField.get(null);

		Field platformField = LuaManager.class.getDeclaredField("platform");
		Platform platform = (Platform)platformField.get(null);

		Field envField = LuaManager.class.getDeclaredField("env");
		KahluaTable env = (KahluaTable)envField.get(null);

		JavaModExposer exposer = JavaModExposer.getInstance(manager, platform, env);
		exposer.exposeJavaMods(JavaModLoader.loadJavaMods(javaModsDir));
	}

	private boolean isLuaManagerReady() throws IllegalAccessException, NoSuchFieldException {
		Field envField = LuaManager.class.getDeclaredField("env");
		KahluaTable env = (KahluaTable)envField.get(null);
		return env != null && env.rawget("Calendar") != null;
	}

	private void waitLuaManagerReady() throws IllegalAccessException, NoSuchFieldException {

		while (!isLuaManagerReady()) {

			try {
				Thread.sleep(1000);
			}
			catch (Exception interrupted) {}
		}
	}

	public void run() {

		try {
			waitLuaManagerReady();
		}
		catch (Exception error) {
			error.printStackTrace();
			throw new RuntimeException(error);
		}

		String installPath = System.getProperty("user.dir");

		File javaModsDir = new File(installPath, "javamods");
		if (!javaModsDir.exists())
			javaModsDir.mkdirs();

		try {
			exposeJavaMods(javaModsDir);
		}
		catch (Exception error) {
			error.printStackTrace();
		}
	}

	private static void startZomboid(String[] args) {
		try {
			final Object[] arg = new Object[]{ args };
			MainScreenState.class.getDeclaredMethod("main", String[].class).invoke(null, arg);
		}
		catch (IllegalAccessException|InvocationTargetException|NoSuchMethodException error) {
			error.printStackTrace();
		}
	}
	public static void main(String[] args) {
		new MainJavaMods();
		startZomboid(args);
	}
}
