package haw.aip3.haw.graph.rating.services;



import haw.aip3.haw.graph.rating.dto.SalesData;
import haw.aip3.haw.graph.rating.dto.SalesDataForOftenSalesPerRegion;
import haw.aip3.haw.graph.rating.dto.SalesDataForRelatedProducts;
import haw.aip3.haw.graph.rating.nodes.AuftragsRelation;
import haw.aip3.haw.graph.rating.nodes.BauteilNode;
import haw.aip3.haw.graph.rating.nodes.GeschaeftspartnerNode;
import haw.aip3.haw.graph.rating.repositories.AuftragsRelationGraphRepository;
import haw.aip3.haw.graph.rating.repositories.BauteilGraphRepository;
import haw.aip3.haw.graph.rating.repositories.GeschaeftspartnerGraphRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RatingServiceImpl implements RatingService {

	  	@Autowired 
	    private GeschaeftspartnerGraphRepository kundeGraphRepository;
	    
	    @Autowired
		private BauteilGraphRepository produktGraphRepository;
	    
	    @Autowired
		private AuftragsRelationGraphRepository auftragsPositionGraphRepository;
		
	    @Autowired
	    private Neo4jTemplate template;

		@Override
		public Iterable<? extends SalesData> showProductSalesByCity(String stadt) {
			Iterable<? extends SalesData> salesData = kundeGraphRepository.showProductSalesByCity(stadt);
			//Iterable<? extends SalesData> salesData = kundeGraphRepository.showProduct(stadt);
			
			return salesData;
		}

		@Override
		public GeschaeftspartnerNode getOrCreateGeschaeftspartner(Long id, String name, String stadt) {
			GeschaeftspartnerNode k = kundeGraphRepository.findByDbid(id);
			if(k==null) {
				k = new GeschaeftspartnerNode(id, name, stadt);
				((Neo4jTemplate) kundeGraphRepository).save(k);
				
//				k = kundeGraphRepository.findOne(k.getId()); // check if it was saved
//				if(k.getDbid()==null) throw new NullPointerException();
//				if(k.getStadt()==null) throw new NullPointerException();
			}
			return k;
		}

		@Override
		public BauteilNode getOrCreateBauteil(Long id, String name) {
			BauteilNode p = produktGraphRepository.findByDbid(id);
			if(p==null) {
				p = new BauteilNode(id, name);
		    	((Neo4jTemplate) produktGraphRepository).save(p);
			}
			return p;
		}

		@Override
		public void addBestellung(GeschaeftspartnerNode k, BauteilNode produkt, double preis) {
			AuftragsRelation r = k.addBestellung(produkt, preis);
			((Neo4jTemplate) auftragsPositionGraphRepository).save(r);
		}

		@Override
		public Iterable<? extends SalesDataForOftenSalesPerRegion> showOftenProductsalesByCity() {
			// TODO Auto-generated method stub
			return kundeGraphRepository.showOftenProductsalesByCity();
		}

		@Override
		public Iterable<? extends SalesDataForRelatedProducts> showRelatedProducts() {
			// TODO Auto-generated method stub
			return kundeGraphRepository.showRelatedProducts();
		}
	
}
