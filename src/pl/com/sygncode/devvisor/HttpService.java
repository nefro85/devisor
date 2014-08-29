package pl.com.sygncode.devvisor;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import android.app.Service;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.IBinder;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class HttpService extends Service {

	private HttpServer server;

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		if (server == null) {
			try {
				server = new HttpServer(9696);
				server.start();
			} catch (IOException e) {
				server = null;
				e.printStackTrace();
			}
		}

		return super.onStartCommand(intent, flags, startId);
	}

	@Override
	public void onDestroy() {
		server.stop();

		super.onDestroy();
	}

	private class HttpServer extends FileServerAdapter {

		private final AssetManager assetManager = getAssets();

		public HttpServer(int port) {
			super(port);
		}

		@Override
		public Response serve(String uri, Method method, Map<String, String> headers, Map<String, String> parms, Map<String, String> files) {

			if (isFileRequest(uri)) {

				try {

					InputStream is = assetManager.open("web" + uri);
					return serveFile(uri, headers, is, getMimeTypeForFile(uri));

				} catch (IOException e) {
					e.printStackTrace();
				}

				return new Response(Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Smolensk exception");
			} else {

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream writer = new PrintStream(out);
				Action action = null;
				if (uri.endsWith(".do")) {
					try {
						action = Action.valueOf(uri.substring(1, uri.indexOf(".")).toUpperCase());

						action.execute(HttpService.this, writer, parms);

					} catch (Exception ex) {
						ex.printStackTrace();

					}
				}
				if (action == null) {

					HtmlBuilder bld = new HtmlBuilder(writer);
					bld.html().body()
							.text("<script src=\"js/jquery-2.0.2.min.js\"></script>" + "\n" + "<script src=\"js/main.js\"></script>" + "<div id=\"root\" />");
					bld.write();

					writer.flush();
					writer.close();
				}
				return new Response(Status.OK, MIME_HTML, new ByteArrayInputStream(out.toByteArray()));
			}
		}

	}

	public enum Action {
		LIST, LOG4MPOWER, EXEC_SQL, PACKAGE_LIST;

		void execute(HttpService ctx, PrintStream out, Map<String, String> parms) throws IOException {
			switch (this) {

			case LOG4MPOWER: {

				File mpowerLog = new File("/data/data/pl.com.softline.formpower/files/log.txt");
				BufferedReader br = new BufferedReader(new FileReader(mpowerLog));
				String line;

				out.print("<pre>");
				while (null != (line = br.readLine())) {
					out.println(line);
				}
				out.print("</pre>");
				br.close();
			}
				break;

			case EXEC_SQL: {

				String pckg = parms.get("package");

				File dbFile = new File("/data/data/" + pckg + "/databases/data.db");
				SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);

				String sql = parms.get("sql");

				Cursor cur = db.rawQuery(sql, null);

				if (cur.moveToFirst()) {
					out.println("<table>");
					String[] columnNames = cur.getColumnNames();

					out.println("<tr>");

					for (String name : columnNames) {
						out.printf("<th>%s</th>", name);
					}

					out.println("</tr>");

					int count = cur.getColumnCount();
					do {
						out.println("<tr>");
						for (int idx = 0; idx < count; idx++) {
							out.printf("<td>%s</td>", String.valueOf(cur.getString(idx)));
						}
						out.println("</tr>");
					} while (cur.moveToNext());
					out.println("</table>");
				}
				cur.close();
				db.close();
			}
				break;

			case PACKAGE_LIST: {
				List<ApplicationInfo> list = ctx.getPackageManager().getInstalledApplications(ctx.getPackageManager().GET_META_DATA);

				out.println("<table>");

				out.println("<tr>");

				for (String name : new String[] { "name", "version" }) {
					out.printf("<th>%s</th>", name);
				}

				out.println("</tr>");

				for (ApplicationInfo info : list) {
					try {
						out.println("<tr>");

						String versionName = ctx.getPackageManager().getPackageInfo(info.packageName, 0).versionName;
						out.printf("<td>%s</td><td>%s</td>", info.packageName, versionName);

						out.println("</tr>");
					} catch (NameNotFoundException e) {
						e.printStackTrace();
					}
				}

				out.println("</table>");

			}
				break;
			}
		}
	}

	public static class HtmlBuilder {

		private PrintStream out;
		private Queue<Element> path = new LinkedList<Element>();

		enum Tag {
			HTML, BODY, TEXT;
		}

		class Element implements ElementWriter {

			public Element(Tag tag, String[] details) {
				this.tag = tag;
				this.details = details;
			}

			public void writeElement(Queue<Element> path) {
				Element r = path.poll();
				if (r != null) {
					switch (tag) {
					default:
						out.println("<" + this.tag.name() + ">");
						r.writeElement(path);
						out.println("</" + this.tag.name() + ">");
					}
				} else {
					switch (tag) {
					case TEXT:
						out.println(this.details[0]);
						break;
					default:
						out.println("<" + this.tag.name() + " />");
						break;
					}
				}
			}

			Tag tag;
			String[] details;
		}

		interface ElementWriter {
			void writeElement(Queue<Element> path);
		}

		public HtmlBuilder(PrintStream out) {
			this.out = out;
		}

		HtmlBuilder html(String... details) {

			Element element = new Element(Tag.HTML, details);
			path.add(element);

			return this;
		}

		HtmlBuilder body(String... details) {
			Element element = new Element(Tag.BODY, details);
			path.add(element);
			return this;
		}

		HtmlBuilder text(String... details) {
			Element element = new Element(Tag.TEXT, details);
			path.add(element);
			return this;
		}

		public void write() {
			while (path.peek() != null) {
				Element e = path.poll();
				e.writeElement(path);
			}
		}

	}
}
