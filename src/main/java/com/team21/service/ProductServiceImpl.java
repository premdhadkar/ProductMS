package com.team21.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.team21.dto.BuyerDTO;
import com.team21.dto.ProductDTO;
import com.team21.dto.SubscribedProductDTO;
import com.team21.entity.ProductEntity;
import com.team21.entity.SubscribedProductEntity;
import com.team21.exception.ProductMSException;
import com.team21.repository.ProductRepository;
import com.team21.repository.SubscribedProductRepository;
import com.team21.utility.CompositeKey;
import com.team21.validator.ProductValidator;

@Transactional
@Service(value = "productService")
public class ProductServiceImpl implements ProductService {

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private SubscribedProductRepository subscribedRepository;

	private static int productCount;

	static {
		productCount = 100;
	}

	// Adding Product to Application
	@Override
	public String addProduct(ProductDTO productDTO) throws ProductMSException {

		ProductEntity productEntity = productRepository.findByProductName(productDTO.getProductName());

		if (productEntity != null)
			throw new ProductMSException("Service.PRODUCT_ALREADY_EXISTS");

		ProductValidator.validateProduct(productDTO);

		productEntity = new ProductEntity();

		String id = "PROD" + productCount++;

		productEntity.setProdId(id);
		productEntity.setProductName(productDTO.getProductName());
		productEntity.setPrice(productDTO.getPrice());
		productEntity.setCategory(productDTO.getCategory());
		productEntity.setDescription(productDTO.getDescription());
		productEntity.setImage(productDTO.getImage());
		productEntity.setSubCategory(productDTO.getSubCategory());
		productEntity.setSellerId(productDTO.getSellerId());
		productEntity.setProductRating(productDTO.getProductRating());
		productEntity.setStock(productDTO.getStock());

		productRepository.save(productEntity);

		return productEntity.getProdId();
	}

	// Delete Product from Application
	@Override
	public String deleteProduct(String prodId) throws ProductMSException {

		ProductEntity productEntity = productRepository.findByProdId(prodId);

		if (productEntity == null)
			throw new ProductMSException("Service.CANNOT_DELETE_PRODUCT");

		productRepository.delete(productEntity);

		return "Product deleted successfully!";

	}

	// Delete All products of a specific seller when id is deactivated
	@Override
	public String deleteProductofDeactiveSeller(String sellerId) {

		List<ProductEntity> productList = productRepository.findAllBysellerId(sellerId);
		if (!productList.isEmpty())

			productRepository.deleteAll(productList);

		return "Products of deactivated seller deleted successfully";

	}

	// Delete all products by specific seller
	@Override
	public void deleteSellerProducts(String sellerId) throws ProductMSException {
		List<ProductEntity> productList = productRepository.findAllBysellerId(sellerId);
		if (!productList.isEmpty()) {
			for (ProductEntity pro : productList) {
				productRepository.delete(pro);
			}
		} else {
			throw new ProductMSException("No product found with the given seller id");
		}
	}

	// Get product by name
	@Override
	public ProductDTO getProductByName(String name) throws ProductMSException {

		ProductEntity product = productRepository.findByProductName(name);
		if (product == null)
			throw new ProductMSException("Service.PRODUCT_DOES_NOT_EXISTS");

		ProductDTO productDTO = ProductDTO.createDTO(product);

		return productDTO;

	}

	// Get product by category
	@Override
	public List<ProductDTO> getProductByCategory(String category) throws ProductMSException {
		List<ProductEntity> productEntities = productRepository.findByCategory(category);

		if (productEntities.isEmpty())
			throw new ProductMSException("Service.PRODUCT_DOES_NOT_EXISTS");

		List<ProductDTO> productDTOs = new ArrayList<>();

		for (ProductEntity product : productEntities) {
			ProductDTO productDTO = ProductDTO.createDTO(product);

			productDTOs.add(productDTO);
		}
		return productDTOs;
	}

	// get product by Id
	@Override
	public ProductDTO getProductById(String id) throws ProductMSException {
		ProductEntity productEntity = productRepository.findByProdId(id);

		if (productEntity == null)
			throw new ProductMSException("Service.PRODUCT_DOES_NOT_EXISTS");

		ProductDTO productDTO = ProductDTO.createDTO(productEntity);

		return productDTO;
	}

	// Reduce Stock after ordering of Products
	@Override
	public Boolean reduceStock(String prodId, Integer quantity) throws ProductMSException {

		Optional<ProductEntity> optional = productRepository.findById(prodId);
		ProductEntity product = optional.orElseThrow(() -> new ProductMSException("Product does not exist"));
		if (product.getStock() >= quantity) {
			product.setStock(product.getStock() - quantity);
			return true;
		}
		return false;
	}

	// Update stock when seller increases stock
	@Override
	public Boolean updateStock(String productId, Integer quantity) throws ProductMSException {
		Optional<ProductEntity> optional = productRepository.findById(productId);
		if (optional.isPresent() == true) {
			if (ProductValidator.validateStock(quantity) == true) {
				optional.get().setStock(quantity);
				productRepository.save(optional.get());
				return true;
			} else
				throw new ProductMSException("Invalid stock value! Minimun stock must be 10.");
		} else
			throw new ProductMSException("No such Product found!!");
	}

	// Get all products
	@Override
	public List<ProductDTO> viewAllProducts() throws ProductMSException {
		List<ProductEntity> productEntities = productRepository.findAll();

		if (productEntities.isEmpty())
			throw new ProductMSException("There are no products to be shown.");

		List<ProductDTO> productDTOs = new ArrayList<>();

		productEntities.forEach(productEntity -> {
			ProductDTO productDTO = ProductDTO.createDTO(productEntity);
			productDTOs.add(productDTO);
		});

		return productDTOs;
	}

	// Adding Subscription for buyer
	@Override
	public String addSubscrption(SubscribedProductDTO subscribedProductDTO, BuyerDTO buyerDTO)
			throws ProductMSException {

		if (buyerDTO.getIsPrivileged().equals("True")) {
			SubscribedProductEntity subscribedEntities = new SubscribedProductEntity();
			CompositeKey key = new CompositeKey(subscribedProductDTO.getBuyerId(), subscribedProductDTO.getProdId());

			subscribedEntities.setCompositeId(key);
			subscribedEntities.setQuantity(subscribedProductDTO.getQuantity());
			subscribedRepository.save(subscribedEntities);
			return "Subscription added successfully!";
		}
		throw new ProductMSException("SERVICE.FAILED_SUBSCRIPTION");

	}

	// get specific subscription details
	@Override
	public SubscribedProductDTO getSubscriptionDetails(String buyerId, String prodId) throws ProductMSException {
		CompositeKey key = new CompositeKey(buyerId, prodId);

		Optional<SubscribedProductEntity> optional = subscribedRepository.findById(key);
		SubscribedProductDTO subscribedproductDTO = null;
		if (optional.isPresent()) {
			SubscribedProductEntity subscribedProduct = optional.get();
			CompositeKey keyFromRepository = subscribedProduct.getCompositeId();
			subscribedproductDTO = new SubscribedProductDTO();

			subscribedproductDTO.setBuyerId(keyFromRepository.getBuyerId());
			subscribedproductDTO.setProdId(keyFromRepository.getProdId());
			subscribedproductDTO.setQuantity(subscribedProduct.getQuantity());

			return subscribedproductDTO;
		} else {
			throw new ProductMSException("SERVICE.SUBSCRIPTION_NOT_FOUND");
		}
	}

}
