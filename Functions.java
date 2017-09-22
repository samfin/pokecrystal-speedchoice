import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Functions {

    public static byte[] readFileFullyIntoBuffer(String filename) throws IOException {
        File fh = new File(filename);
        if (!fh.exists() || !fh.isFile() || !fh.canRead()) {
            throw new FileNotFoundException(filename);
        }
        long fileSize = fh.length();
        if (fileSize > Integer.MAX_VALUE) {
            throw new IOException(filename + " is too long to read in as a byte-array.");
        }
        FileInputStream fis = new FileInputStream(filename);
        byte[] buf = readFullyIntoBuffer(fis, (int) fileSize);
        fis.close();
        return buf;
    }

    public static byte[] readFullyIntoBuffer(InputStream in, int bytes) throws IOException {
        byte[] buf = new byte[bytes];
        readFully(in, buf, 0, bytes);
        return buf;
    }

    public static void readFully(InputStream in, byte[] buf, int offset, int length) throws IOException {
        int offs = 0, read = 0;
        while (offs < length && (read = in.read(buf, offs + offset, length - offs)) != -1) {
            offs += read;
        }
    }

	public static void writeFile(String filename, byte[] data) {

		writeFile(filename, data, 0, data.length);

	}

	public static void writeFile(String filename, byte[] data, int offset,
			int length) {
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(data, offset, length);
			fos.close();
		} catch (IOException e) {
		}
	}

	public static int readWord(byte[] data, int offset) {
		return (data[offset] & 0xFF) + ((data[offset + 1] & 0xFF) << 8);
	}

	public static int readLong(byte[] data, int offset) {
		return (data[offset] & 0xFF) + ((data[offset + 1] & 0xFF) << 8)
				+ ((data[offset + 2] & 0xFF) << 16)
				+ ((data[offset + 3] & 0xFF) << 24);
	}

	public static void writeWord(byte[] data, int offset, int value) {
		data[offset] = (byte) (value & 0xFF);
		data[offset + 1] = (byte) ((value / 0x100) & 0xFF);
	}

	public static void writeLong(byte[] data, int offset, int value) {
		data[offset] = (byte) (value & 0xFF);
		data[offset + 1] = (byte) ((value >> 8) & 0xFF);
		data[offset + 2] = (byte) ((value >> 16) & 0xFF);
		data[offset + 3] = (byte) ((value >> 24) & 0xFF);
	}

	public static int readPointer(byte[] data, int offset) {
		return readLong(data, offset) - 0x8000000;
	}

	public static void writePointer(byte[] data, int offset, int pointer) {
		writeLong(data, offset, pointer + 0x8000000);
	}

	public static int arraySum(int[] arr) {
		return arraySum(arr, 0, arr.length);
	}

	public static int arraySum(int[] arr, int offset, int length) {
		int sum = 0;
		for (int i = 0; i < length; i++) {
			sum += arr[i + offset];
		}
		return sum;
	}

	public static int[] deltas = new int[] { 0, 1, 4, 9, 16, 25, 36, 49, 192,
			207, 220, 231, 240, 247, 252, 255 };

	public static int pickDelta(int baseByte, int newByte) {
		int trueDelta = trueMod256(newByte - baseByte);
		int deltaIndex = -1;
		int deltaScore = 99999;
		for (int i = 0; i < 16; i++) {
			int newDS = Math.abs(trueDelta - deltas[i]);
			if (newDS < deltaScore) {
				deltaIndex = i;
				deltaScore = newDS;
			}
		}
		return deltaIndex;
	}

	public static int trueMod256(int val) {
		val = val % 256;
		if (val < 0) {
			val += 256;
		}
		return val;
	}

	public static int updateBase(int base, int deltaIndex) {
		return trueMod256(base + deltas[deltaIndex]);
	}

	public static String[] tb;
	public static Map<String, Byte> d;
	public static final int textTerminator = 0xFF, textVariable = 0xFD;

	static {
		tb = new String[256];
		d = new HashMap<String, Byte>();
		loadTextTable();
	}

	private static void loadTextTable() {
		try {
			Scanner sc = new Scanner(new File("gba_english.tbl"), "UTF-8");
			while (sc.hasNextLine()) {
				String q = sc.nextLine();
				if (!q.trim().isEmpty()) {
					String[] r = q.split("=", 2);
					if (r[1].endsWith("\r\n")) {
						r[1] = r[1].substring(0, r[1].length() - 2);
					}
					tb[Integer.parseInt(r[0], 16)] = r[1];
					d.put(r[1], (byte) Integer.parseInt(r[0], 16));
				}
			}
			sc.close();
		} catch (FileNotFoundException e) {
		}

	}

	private static String readString(byte[] rom, int offset, int maxLength) {
		StringBuilder string = new StringBuilder();
		for (int c = 0; c < maxLength; c++) {
			int currChar = rom[offset + c] & 0xFF;
			if (tb[currChar] != null) {
				string.append(tb[currChar]);
			} else {
				if (currChar == textTerminator) {
					break;
				} else if (currChar == textVariable) {
					int nextChar = rom[offset + c + 1] & 0xFF;
					string.append("\\v" + String.format("%02X", nextChar));
					c++;
				} else {
					string.append("\\x" + String.format("%02X", currChar));
				}
			}
		}
		return string.toString();
	}

	private static byte[] translateString(byte[] rom, String text) {
		List<Byte> data = new ArrayList<Byte>();
		while (text.length() != 0) {
			int i = Math.max(0, 4 - text.length());
			if (text.charAt(0) == '\\' && text.charAt(1) == 'x') {
				data.add((byte) Integer.parseInt(text.substring(2, 4), 16));
				text = text.substring(4);
			} else if (text.charAt(0) == '\\' && text.charAt(1) == 'v') {
				data.add((byte) textVariable);
				data.add((byte) Integer.parseInt(text.substring(2, 4), 16));
				text = text.substring(4);
			} else {
				while (!(d.containsKey(text.substring(0, 4 - i)) || (i == 4))) {
					i++;
				}
				if (i == 4) {
					text = text.substring(1);
				} else {
					data.add(d.get(text.substring(0, 4 - i)));
					text = text.substring(4 - i);
				}
			}
		}
		byte[] ret = new byte[data.size()];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = data.get(i);
		}
		return ret;
	}

	public static String readFixedLengthString(byte[] rom, int offset,
			int length) {
		return readString(rom, offset, length);
	}

	public static String readVariableLengthString(byte[] rom, int offset) {
		return readString(rom, offset, Integer.MAX_VALUE);
	}

	public static void writeFixedLengthString(byte[] rom, String str,
			int offset, int length) {
		byte[] translated = translateString(rom, str);
		int len = Math.min(translated.length, length);
		System.arraycopy(translated, 0, rom, offset, len);
		if (len < length) {
			rom[offset + len] = (byte) textTerminator;
			len++;
		}
		while (len < length) {
			rom[offset + len] = 0;
			len++;
		}
	}

	public static void writeVariableLengthString(byte[] rom, String str,
			int offset) {
		byte[] translated = translateString(rom, str);
		System.arraycopy(translated, 0, rom, offset, translated.length);
		rom[offset + translated.length] = (byte) 0xFF;
	}

	public static int lengthOfStringAt(byte[] rom, int offset) {
		int len = 0;
		while ((rom[offset + (len++)] & 0xFF) != 0xFF) {
		}
		return len - 1;
	}

	public static byte[] traduire(byte[] rom, String str) {
		return translateString(rom, str);
	}

	public static List<Integer> search(byte[] haystack, byte[] needle) {
		return search(haystack, 0, haystack.length, needle, 1);
	}

	public static List<Integer> search4(byte[] haystack, byte[] needle) {
		return search(haystack, 0, haystack.length, needle, 4);
	}

	public static List<Integer> search(byte[] haystack, int beginOffset,
			byte[] needle) {
		return search(haystack, beginOffset, haystack.length, needle, 1);
	}

	public static List<Integer> search(byte[] haystack, int beginOffset,
			int endOffset, byte[] needle, int resultAlignment) {
		int currentMatchStart = beginOffset;
		int currentCharacterPosition = 0;

		int docSize = endOffset;
		int needleSize = needle.length;

		int[] toFillTable = buildKMPSearchTable(needle);
		List<Integer> results = new ArrayList<Integer>();

		while ((currentMatchStart + currentCharacterPosition) < docSize) {

			if (needle[currentCharacterPosition] == (haystack[currentCharacterPosition
					+ currentMatchStart])) {
				currentCharacterPosition = currentCharacterPosition + 1;

				if (currentCharacterPosition == (needleSize)) {
					if (currentMatchStart % resultAlignment == 0) {
						results.add(currentMatchStart);
					}
					currentCharacterPosition = 0;
					currentMatchStart = currentMatchStart + needleSize;

				}

			} else {
				currentMatchStart = currentMatchStart
						+ currentCharacterPosition
						- toFillTable[currentCharacterPosition];

				if (toFillTable[currentCharacterPosition] > -1) {
					currentCharacterPosition = toFillTable[currentCharacterPosition];
				}

				else {
					currentCharacterPosition = 0;

				}

			}
		}
		return results;
	}

	private static int[] buildKMPSearchTable(byte[] needle) {
		int[] stable = new int[needle.length];
		int pos = 2;
		int j = 0;
		stable[0] = -1;
		if (needle.length >= 2)
			stable[1] = 0;
		while (pos < needle.length) {
			if (needle[pos - 1] == needle[j]) {
				stable[pos] = j + 1;
				pos++;
				j++;
			} else if (j > 0) {
				j = stable[j];
			} else {
				stable[pos] = 0;
				pos++;
			}
		}
		return stable;
	}

}
