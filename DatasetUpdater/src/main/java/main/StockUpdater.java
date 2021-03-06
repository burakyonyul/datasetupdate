package main;

import static main.LogFieldFormatter.format;
import static main.LogFieldFormatter.pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.joda.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra;

import virtuoso.jena.driver.VirtGraph;
import virtuoso.jena.driver.VirtuosoQueryExecution;
import virtuoso.jena.driver.VirtuosoQueryExecutionFactory;

public class StockUpdater extends Thread {

	ArrayList<String> dbpediaCompanyList = new ArrayList<String>();
	ArrayList<String> nytimesCompanyList = new ArrayList<String>();
	ArrayList<Integer> articleCount = new ArrayList<Integer>();
	ArrayList<VirtGraph> storeList = new ArrayList<VirtGraph>();

	private Logger logger = LoggerFactory.getLogger(StockUpdater.class);

	private void init() throws IOException

	{

		storeList.add(new VirtGraph("http://stockmarket.com", "jdbc:virtuoso://155.223.24.193:1111", "dba", "dba"));

		InputStream is = getClass().getResourceAsStream("/organization_data.txt");
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		String line;
		int blankPosition;
		while ((line = br.readLine()) != null) {
			blankPosition = line.indexOf(" ");
			this.dbpediaCompanyList.add(line.substring(0, blankPosition));
			line = line.substring(blankPosition + 1);
			blankPosition = line.indexOf(" ");
			this.nytimesCompanyList.add(line.substring(0, blankPosition));
			line = line.substring(blankPosition + 1);
			this.articleCount.add(Integer.parseInt(line));

		}

	}

	public void run() {

		try {
			this.init();

			int queryCounter = 0;

			logger.debug(format(pair("time", LocalDateTime.now()), pair("dataset", "stock")),
					"All datasets are being updated");

			while (queryCounter < this.nytimesCompanyList.size()) {

				/*
				 * try { out= new FileOutputStream("/home/oylum/Desktop/stock1/stock_output"+
				 * queryCounter+".txt",true); pSOut=new PrintStream(out);
				 * 
				 * } catch (FileNotFoundException e) {
				 * 
				 * e.printStackTrace(); }
				 */

				logger.debug(format(pair("time", LocalDateTime.now()), pair("dataset", "stock")),
						"Dataset update started");
				queryCounter++;

				Node firstPredicate = NodeFactory.createURI("http://stockmarket.com/elements/stockValue");
				for (int i = 0; i < this.nytimesCompanyList.size(); i++) {

					Node subject = NodeFactory.createURI(this.nytimesCompanyList.get(i));

					Query sparql = QueryFactory.create("SELECT ?stock WHERE { <" + subject.getURI() + "> <"
							+ firstPredicate.getURI() + "> ?stock }");

					VirtuosoQueryExecution vqe = VirtuosoQueryExecutionFactory.create(sparql, storeList.get(0));
					int stock = 0;
					ResultSet results = vqe.execSelect();
					while (results.hasNext()) {
						QuerySolution result = results.nextSolution();
						stock = result.get("stock").asLiteral().getInt();

						storeList.get(0).delete(new Triple(subject, firstPredicate, NodeFactoryExtra.intToNode(stock)));

					}
					vqe.close();
					stock++;
					storeList.get(0).add(new Triple(subject, firstPredicate, NodeFactoryExtra.intToNode(stock)));
					logger.debug(format(pair("time", LocalDateTime.now()), pair("company", subject.getURI()),
							pair("dataset", "stock")), "Company data has been updated");
				}

				logger.debug(format(pair("time", LocalDateTime.now()), pair("dataset", "stock")),
						"Dataset update ended");
				Thread.sleep(180000);

			}

			logger.debug(format(pair("time", LocalDateTime.now()), pair("dataset", "stock")),
					"All datasets has been updated");

		} catch (Exception ex) {
			logger.error(ex.getMessage());
		}

	}

}
