import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

public class JsonReader {
	public static void main(String... args) throws IOException {
		

		File csvFile = new File("json_results.txt");
		if (!csvFile.exists()) {
			try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
				writer.write(String.join("\t", "APK", "#Flows"));
				writer.newLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile, true))) {
			StringBuilder sb = new StringBuilder();
			File folder = new File("jsonoutput/");
			for (File fileEntry : folder.listFiles()) {
				sb.append(fileEntry.getName());
				sb.append("\t");
				String content = FileUtils.readFileToString(fileEntry, "utf-8");
				JSONObject obj = new JSONObject(content);
				System.out.println(obj.getJSONArray("Results").length());
				sb.append(obj.getJSONArray("Results").length());
				sb.append("\n");
			}
			writer.write(sb.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
