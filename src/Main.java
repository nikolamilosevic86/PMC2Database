import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

public class Main {
	public static Connection conn = null;
	public static Statement stmt = null;
	ResultSet rs = null;

	public static void DataBaseSaver() {
		try {
			BufferedReader br = new BufferedReader(new FileReader("database"));
			// StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			String host = "";
			String database_name = "";
			String database_username = "";
			String database_password = "";
			String database_port = "";
			while (line != null && line != "") {
				KeyValue kv = new KeyValue();
				String[] parts = line.split(":");
				kv.key = parts[0];
				kv.value = parts[1];
				if (kv.key.equals("host")) {
					host = kv.value;
				}
				if (kv.key.equals("database")) {
					database_name = kv.value;
				}
				if (kv.key.equals("username")) {
					database_username = kv.value;
				}
				if (kv.key.equals("password")) {
					database_password = kv.value;
				}
				if (kv.key.equals("port")) {
					database_port = kv.value;
				}
				line = br.readLine();
			}

			database_password = database_password.replace("\"", "");
			database_password = database_password.replace(" ", "");
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			String connectionUrl = "jdbc:mysql://" + host + ":" + database_port
					+ "/" + database_name;
			String connectionUser = database_username;
			String connectionPassword = database_password;
			conn = DriverManager.getConnection(connectionUrl, connectionUser,
					connectionPassword);
			stmt = conn.createStatement();
		} catch (SQLException ex) {
			ex.printStackTrace();
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLState: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String path = args[0];
		System.out.println("Path:" + path);
		DataBaseSaver();
		File[] files = (new File(path)).listFiles();
		for (File folder : files) {
			File[] XMLfiles = (new File(folder.getAbsolutePath())).listFiles();
			for (File XMLFile : XMLfiles) {
				if (!XMLFile.getName().contains("nxml")) {
					continue;
				}
				Article art = new Article(XMLFile.getName());
				art.setSource("PMC");
				try {
					@SuppressWarnings("resource")
					FileReader fr = new FileReader(XMLFile);
					BufferedReader reader = new BufferedReader(fr);
					String line = null;
					String xml = "";
					while ((line = reader.readLine()) != null) {
						if (line.contains("JATS-archivearticle1.dtd")
								|| line.contains("archivearticle.dtd"))
							continue;
						xml += line + '\n';
					}
					DocumentBuilderFactory factory = DocumentBuilderFactory
							.newInstance();

					factory.setNamespaceAware(true);
					factory.setValidating(false);
					DocumentBuilder builder = factory.newDocumentBuilder();
					InputSource is = new InputSource(new StringReader(xml));
					Document parse = builder.parse(is);
					art = ParseMetaData(art, parse, xml);

					Statement stmt = conn.createStatement();
					String insertTableSQL = "INSERT INTO pmc_articles_2017 (PMCid,Title,PMid,Long_abstract,Short_Abstract,XML,publisher_name,publisher_loc,journal_name,year) VALUES (?,?,?,?,?,?,?,?,?,?)";
					PreparedStatement preparedStatement = conn
							.prepareStatement(insertTableSQL,
									Statement.RETURN_GENERATED_KEYS);
					preparedStatement.setString(1, art.getPmc());
					preparedStatement.setString(2, art.getTitle());
					preparedStatement.setString(3, art.getPmid());
					preparedStatement.setString(4, art.getAbstract());
					preparedStatement.setString(5, art.getShort_abstract());
					preparedStatement.setString(6, art.getXML());
					preparedStatement.setString(7, art.getPublisher_name());
					preparedStatement.setString(8, art.getPublisher_loc());
					preparedStatement.setString(9, art.getJournal_name());
					preparedStatement.setString(10, art.getYear());
					// execute insert SQL stetement
					int articleId = preparedStatement.executeUpdate();
					fr.close();
					reader.close();
					stmt.close();

				} catch (SAXParseException sex) {
					sex.printStackTrace();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

			}
		}

	}

	public static Article ParseMetaData(Article art, Document parse, String xml) {
		String title = "";
		String journal = "";
		if (parse.getElementsByTagName("article-title") != null
				&& parse.getElementsByTagName("article-title").item(0) != null) {
			title = parse.getElementsByTagName("article-title").item(0)
					.getTextContent();
			title = title.replaceAll("\n", "");
			title = title.replaceAll("\t", "");
			System.out.println(title);
		}

		// journal-title
		if (parse.getElementsByTagName("journal-title") != null
				&& parse.getElementsByTagName("journal-title").item(0) != null) {
			journal = parse.getElementsByTagName("journal-title").item(0)
					.getTextContent();
			journal = journal.replaceAll("\n", "");
			journal = journal.replaceAll("\t", "");
		}
		if (parse.getElementsByTagName("pub-date") != null
				&& parse.getElementsByTagName("pub-date").item(0) != null) {
			Node pubdate = parse.getElementsByTagName("pub-date").item(0);
			for (int l = 0; l < pubdate.getChildNodes().getLength(); l++) {
				if (pubdate.getChildNodes().item(l).getNodeName()
						.equals("year"))
					art.setYear(pubdate.getChildNodes().item(l)
							.getTextContent());
			}
		}

		NodeList issn = parse.getElementsByTagName("issn");
		for (int j = 0; j < issn.getLength(); j++) {
			if (issn == null
					|| issn.item(j) == null
					|| issn.item(j).getAttributes() == null
					|| issn.item(j).getAttributes().getNamedItem("pub-type") == null
					|| issn.item(j).getAttributes().getNamedItem("pub-type")
							.getNodeValue() == null)
				continue;
			if (issn.item(j).getAttributes().getNamedItem("pub-type")
					.getNodeValue().equals("ppub")) {
				String issnp = issn.item(j).getTextContent();
				art.setPissn(issnp);
				if (issnp != null)
					System.out.println(issnp);
			}
			if (issn.item(j).getAttributes().getNamedItem("pub-type")
					.getNodeValue().equals("epub")) {
				String issne = issn.item(j).getTextContent();
				art.setPissn(issne);
				if (issne != null)
					System.out.println(issne);
			}
		}
		NodeList article_id = parse.getElementsByTagName("article-id");
		for (int j = 0; j < article_id.getLength(); j++) {
			if (article_id.item(j).getAttributes() != null
					&& article_id.item(j).getAttributes()
							.getNamedItem("pub-id-type") != null
					&& article_id.item(j).getAttributes()
							.getNamedItem("pub-id-type").getNodeValue()
							.equals("pmid")) {
				String pmid = article_id.item(j).getTextContent();
				art.setPmid(pmid);
				if (pmid != null)
					System.out.println(pmid);
			}
			if (article_id.item(j).getAttributes() != null
					&& article_id.item(j).getAttributes()
							.getNamedItem("pub-id-type") != null
					&& article_id.item(j).getAttributes()
							.getNamedItem("pub-id-type").getNodeValue()
							.equals("pmc")) {
				String pmc = article_id.item(j).getTextContent();
				art.setPmc(pmc);
				art.setSpec_id(pmc);
				if (pmc != null)
					System.out.println(pmc);
			}
		}

		NodeList art_abstract = parse.getElementsByTagName("abstract");
		for (int j = 0; j < art_abstract.getLength(); j++) {
			if (art_abstract.item(j).getAttributes()
					.getNamedItem("abstract-type") != null
					&& art_abstract.item(j).getAttributes()
							.getNamedItem("abstract-type").getNodeValue()
							.equals("short")) {
				art.setShort_abstract(art_abstract.item(j).getTextContent());
			} else {
				art.setAbstract(art_abstract.item(j).getTextContent());
			}
		}

		String publisher_name = "";
		if (parse.getElementsByTagName("publisher-name").item(0) != null)
			publisher_name = parse.getElementsByTagName("publisher-name")
					.item(0).getTextContent();
		art.setPublisher_name(publisher_name);
		if (publisher_name != null)
			System.out.println(publisher_name);
		String publisher_loc = "";
		if (parse.getElementsByTagName("publisher-loc").item(0) != null)
			publisher_loc = parse.getElementsByTagName("publisher-loc").item(0)
					.getTextContent();
		art.setPublisher_loc(publisher_loc);
		if (publisher_loc != null)
			System.out.println(publisher_loc);
		try {
			if (parse.getElementsByTagName("body").item(0) != null) {
				String plain_text = parse.getElementsByTagName("body").item(0)
						.getTextContent();
				art.setPlain_text(plain_text);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		art.setTitle(title);
		art.setXML(xml);
		art.setJournal_name(journal);
		return art;
	}

}
