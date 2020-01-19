package net.razorvine.pickle.test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.razorvine.pickle.IObjectConstructor;
import net.razorvine.pickle.PickleException;
import net.razorvine.pickle.PythonException;
import net.razorvine.pickle.Unpickler;
import net.razorvine.pickle.objects.ClassDict;
import net.razorvine.pickle.objects.ClassDictConstructor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for some more complex unpickler objects (PyroProxy).
 *
 * @author Irmen de Jong (irmen@razorvine.net)
 */
@SuppressWarnings({"unchecked", "serial"})
public class UnpicklerComplexTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}


	@Test
	public void testUnpickleProto0Bytes() throws IOException, NoSuchAlgorithmException {
		InputStream is = this.getClass().getResourceAsStream("pickled_bytes_level0.dat");
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		int c;
		while((c = is.read())>=0) bos.write(c);
		is.close();
		byte[] pickle = bos.toByteArray();

		PickleSerializer ser = new PickleSerializer();
		String x = (String)ser.deserializeData(pickle);
		assertEquals(2496, x.length());

		// validate that the bytes in the string are what we expect (based on md5 hash)
		MessageDigest m = MessageDigest.getInstance("SHA1");
		m.update(x.getBytes("utf-8"));
		BigInteger digest = new BigInteger(1, m.digest());
		assertEquals("22f45b876383c91b1cb20afe51ee3b30f5a85d4c", digest.toString(16));
	}

	@Test
	public void testUnpickleMemo() throws PickleException, IOException {
		// the pickle is of the following list: [65, 'hello', 'hello', {'recurse': [...]}, 'hello']
		// i.e. the 4th element is a dict referring back to the list itself and the 'hello' strings are reused
		byte[] pickle = new byte[]
			{(byte) 128, 2, 93, 113, 0, 40, 75, 65, 85, 5, 104, 101, 108, 108, 111, 113, 1, 104, 1, 125, 113, 2,
			85, 7, 114, 101, 99, 117, 114, 115, 101, 113, 3, 104, 0, 115, 104, 1, 101, 46};
		PyroSerializer ser = new PickleSerializer();
		ArrayList<Object> a = (ArrayList<Object>) ser.deserializeData(pickle);
		assertEquals(5, a.size());
		assertEquals(65, a.get(0));
		assertEquals("hello", a.get(1));
		assertSame(a.get(1), a.get(2));
		assertSame(a.get(1), a.get(4));
		HashMap<String, Object> h = (HashMap<String,Object>) a.get(3);
		assertSame(a, h.get("recurse"));
	}

	@Test
	public void testUnpickleUnsupportedClass() throws IOException {
		// an unsupported class is mapped to a dictionary containing the class's attributes, and a __class__ attribute with the name of the class
		byte[] pickled = new byte[] {
				(byte)128, 2, 99, 95, 95, 109, 97, 105, 110, 95, 95, 10, 67, 117, 115, 116, 111, 109, 67, 108,
				97, 115, 115, 10, 113, 0, 41, (byte)129, 113, 1, 125, 113, 2, 40, 85, 3, 97, 103, 101, 113, 3,
				75, 34, 85, 6, 118, 97, 108, 117, 101, 115, 113, 4, 93, 113, 5, 40, 75, 1, 75, 2, 75, 3,
				101, 85, 4, 110, 97, 109, 101, 113, 6, 85, 5, 72, 97, 114, 114, 121, 113, 7, 117, 98, 46};

		PyroSerializer ser = new PickleSerializer();
		Map<String, Object> o = (Map<String, Object>) ser.deserializeData(pickled);
		assertEquals(4, o.size());
		assertEquals("Harry", o.get("name"));
		assertEquals(34, o.get("age"));
		ArrayList<Object> expected = new ArrayList<Object>() {{
			add(1);
			add(2);
			add(3);
		}};
		assertEquals(expected, o.get("values"));
		assertEquals("__main__.CustomClass", o.get("__class__"));
	}


	public class CustomClazz {
		public String name;
		public int age;
		public ArrayList<Object> values;
		public CustomClazz()
		{

		}
		public CustomClazz(String name, int age, ArrayList<Object> values)
		{
			this.name=name;
			this.age=age;
			this.values=values;
		}

		/**
		 * called by the Unpickler to restore state.
		 */
		public void __setstate__(HashMap<String, Object> args) {
			this.name = (String) args.get("name");
			this.age = (Integer) args.get("age");
			this.values = (ArrayList<Object>) args.get("values");
		}
	}

	class CustomClazzConstructor implements IObjectConstructor
	{
		public Object construct(Object[] args) throws PickleException
		{
			if(args.length==0)
			{
				return new CustomClazz();    // default constructor
			}
			else if(args.length==3)
			{
				String name = (String)args[0];
				int age = (Integer) args[1];
				ArrayList<Object> values = (ArrayList<Object>) args[2];
				return new CustomClazz(name, age, values);
			}
			else throw new PickleException("expected 0 or 3 constructor arguments");
		}
	}

	@Test
	public void testUnpickleCustomClassAsClassDict() throws IOException {
		byte[] pickled = new byte[] {
				(byte)128, 2, 99, 95, 95, 109, 97, 105, 110, 95, 95, 10, 67, 117, 115, 115, 115, 115, 115, 115,
				97, 122, 122, 10, 113, 0, 41, (byte)129, 113, 1, 125, 113, 2, 40, 85, 3, 97, 103, 101, 113, 3,
				75, 34, 85, 6, 118, 97, 108, 117, 101, 115, 113, 4, 93, 113, 5, 40, 75, 1, 75, 2, 75, 3,
				101, 85, 4, 110, 97, 109, 101, 113, 6, 85, 5, 72, 97, 114, 114, 121, 113, 7, 117, 98, 46};

		PyroSerializer ser = new PickleSerializer();
		ClassDict cd = (ClassDict) ser.deserializeData(pickled);
		assertEquals("__main__.Cussssssazz", cd.get("__class__"));
		assertEquals("Harry", cd.get("name"));
		assertEquals(34, cd.get("age"));
		ArrayList<Object> expected = new ArrayList<Object>() {{
			add(1);
			add(2);
			add(3);
		}};
		assertEquals(expected, cd.get("values"));
	}

	@Test
	public void testClassDictConstructorSetsClass() {
		ClassDict cd = new ClassDict("module", "myclass");
		assertEquals("module.myclass", cd.get("__class__"));

		ClassDictConstructor cdc = new ClassDictConstructor("module", "myclass");
		cd = (ClassDict) cdc.construct(new Object[]{});
		assertEquals("module.myclass", cd.get("__class__"));

		assertEquals("module.myclass", cd.getClassName());
	}

	@Test
	public void testUnpickleCustomClass() throws IOException {
		byte[] pickled = new byte[] {
				(byte)128, 2, 99, 95, 95, 109, 97, 105, 110, 95, 95, 10, 67, 117, 115, 116, 111, 109, 67, 108,
				97, 122, 122, 10, 113, 0, 41, (byte)129, 113, 1, 125, 113, 2, 40, 85, 3, 97, 103, 101, 113, 3,
				75, 34, 85, 6, 118, 97, 108, 117, 101, 115, 113, 4, 93, 113, 5, 40, 75, 1, 75, 2, 75, 3,
				101, 85, 4, 110, 97, 109, 101, 113, 6, 85, 5, 72, 97, 114, 114, 121, 113, 7, 117, 98, 46};

		Unpickler.registerConstructor("__main__","CustomClazz", new CustomClazzConstructor());
		PyroSerializer ser = new PickleSerializer();
		CustomClazz o = (CustomClazz) ser.deserializeData(pickled);
		assertEquals("Harry" ,o.name);
		assertEquals(34 ,o.age);
		ArrayList<Object> expected = new ArrayList<Object>() {{
			add(1);
			add(2);
			add(3);
		}};
		assertEquals(expected, o.values);
	}

	@Test
	public void testUnpickleException() throws IOException {
		PyroSerializer ser = new PickleSerializer();

		// python 2.x
		PythonException x = (PythonException) ser.deserializeData("cexceptions\nZeroDivisionError\np0\n(S'hello'\np1\ntp2\nRp3\n.".getBytes());
		assertEquals("[exceptions.ZeroDivisionError] hello", x.getMessage());
		assertEquals("exceptions.ZeroDivisionError", x.pythonExceptionType);

		// python 3.x
		x = (PythonException) ser.deserializeData("c__builtin__\nZeroDivisionError\np0\n(Vhello\np1\ntp2\nRp3\n.".getBytes());
		assertEquals("[__builtin__.ZeroDivisionError] hello", x.getMessage());
		assertEquals("__builtin__.ZeroDivisionError", x.pythonExceptionType);
		x = (PythonException) ser.deserializeData("cbuiltins\nZeroDivisionError\np0\n(Vhello\np1\ntp2\nRp3\n.".getBytes());
		assertEquals("[builtins.ZeroDivisionError] hello", x.getMessage());
		assertEquals("builtins.ZeroDivisionError", x.pythonExceptionType);

		// python 2.x
		x = (PythonException) ser.deserializeData("cexceptions\nGeneratorExit\np0\n(tRp1\n.".getBytes());
		assertEquals("[exceptions.GeneratorExit]", x.getMessage());
		assertEquals("exceptions.GeneratorExit", x.pythonExceptionType);
		// python 3.x
		x = (PythonException) ser.deserializeData("c__builtin__\nGeneratorExit\np0\n(tRp1\n.".getBytes());
		assertEquals("[__builtin__.GeneratorExit]", x.getMessage());
		assertEquals("__builtin__.GeneratorExit", x.pythonExceptionType);
		x = (PythonException) ser.deserializeData("cbuiltins\nGeneratorExit\np0\n(tRp1\n.".getBytes());
		assertEquals("[builtins.GeneratorExit]", x.getMessage());
		assertEquals("builtins.GeneratorExit", x.pythonExceptionType);
	}
}
