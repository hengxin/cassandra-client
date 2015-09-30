/**
 * Create (if not exists) picturedb table, insert some pictures, 
 * and issues some queries. 
 */
package exercise;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.Metadata;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.Bytes;

/**
 * @author hengxin
 * @date 09-30-2015
 */
public class ImageDBClient {
	public static String KEYSPACE_IMAGEDB = "imagedb"; 
	public static String TABLE_PICTURES = "images"; 
	public static String KEYSPACE_DOT_TABLE = "imagedb.images";
	
	private UUID uuid;
	private String uuid_Str;
	
	private Cluster cluster;
	private Session session;

	public Session getSession() {
		return this.session;
	}

	public void connect(String node) {
		cluster = Cluster.builder().addContactPoint(node).build();
		Metadata metadata = cluster.getMetadata();
		System.out.printf("Connected to cluster: %s\n", metadata.getClusterName());
		for ( Host host : metadata.getAllHosts() ) {
			System.out.printf("Datatacenter: %s; Host: %s; Rack: %s\n",
					host.getDatacenter(), host.getAddress(), host.getRack());
		}
		session = cluster.connect();
	}

	/*
	 * create keyspace "picturedb" if not exists
	 * create table "picturedb.pictures" if not exits
	 */
	public void createSchema() {
		session.execute("CREATE KEYSPACE IF NOT EXISTS " + ImageDBClient.KEYSPACE_IMAGEDB + " WITH replication " + 
				"= {'class' : 'NetworkTopologyStrategy', 'datacenter1' : 3};");

		session.execute(
				"CREATE TABLE IF NOT EXISTS " + ImageDBClient.KEYSPACE_DOT_TABLE + " (" +
						"id uuid PRIMARY KEY," + 
						"title text," + 
						"album text," + 
						"artist text," + 
						"tags set<text>," + 
						"data blob" + 
				");");
/*		session.execute(
				"CREATE TABLE IF NOT EXISTS simplex.playlists (" +
						"id uuid," +
						"title text," +
						"album text, " + 
						"artist text," +
						"song_id uuid," +
						"PRIMARY KEY (id, title, album, artist)" +
				");");*/
	}

	public void loadData() throws IOException {
		// prepare an image
		File image_file = new File("sources/images/dynamo-logo.jpg");
		FileInputStream fis = new FileInputStream(image_file);
		byte[] image_blob = new byte[(int) image_file.length()];
		fis.read(image_blob);
		fis.close();
		
		// uuid as the id
		uuid = UUID.randomUUID();
		uuid_Str = uuid.toString();
		
		session.execute(
				"INSERT INTO " + ImageDBClient.KEYSPACE_DOT_TABLE + " (id, title, album, artist, tags, data) " +
						"VALUES (" +
						uuid_Str + "," + 
						"'dynamo-logo.jpg'," +
						"'distributed storage systems'," +
						"'Amazon'," +
						"{'dss', 'dsm'}, " +
						Bytes.toHexString(image_blob) +
				");");
/*		session.execute(
				"INSERT INTO simplex.playlists (id, song_id, title, album, artist) " +
						"VALUES (" +
						"2cc9ccb7-6221-4ccb-8387-f22b6a1b354d," +
						"756716f7-2e54-4715-9f00-91dcbea6cf50," +
						"'La Petite Tonkinoise'," +
						"'Bye Bye Blackbird'," +
						"'Joséphine Baker'" +
				");");*/
	}

	public void querySchema() throws IOException {
		ResultSet results = session.execute("SELECT * FROM  " + ImageDBClient.KEYSPACE_DOT_TABLE +
				" WHERE id = " + uuid_Str + ";");
		System.out.println(String.format("%-30s\t%-20s\t%-20s\n%s", "title", "album", "artist",
				"-------------------------------+-----------------------+--------------------"));
		
		
		for (Row row : results) {
			System.out.println(String.format("%-30s\t%-20s\t%-20s", row.getString("title"),
					row.getString("album"),  row.getString("artist")));
             
			ByteBuffer image_data = row.getBytes("data");
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(Bytes.getArray(image_data)));
			
			System.out.println(image.getType());
			
			// Use a label to display the image
			JFrame frame = new JFrame();

			JLabel image_lbl = new JLabel(new ImageIcon(image));
			
			JPanel image_panel = new JPanel(new BorderLayout());
			image_panel.add(image_lbl);

			frame.add(image_panel);
			frame.setSize(300, 400);
			frame.setVisible(true);
		}
		
		System.out.println();
	}

	public void close() {
		session.close();
		cluster.close();
	}

	public static void main(String[] args) throws IOException {
		ImageDBClient client = new ImageDBClient();
		client.connect("172.17.0.1");
		client.createSchema();
		client.loadData();
		client.querySchema();
		client.close();
	}
}
