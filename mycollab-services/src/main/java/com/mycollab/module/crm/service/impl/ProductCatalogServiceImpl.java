/**
 * This file is part of mycollab-services.
 *
 * mycollab-services is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * mycollab-services is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with mycollab-services.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.mycollab.module.crm.service.impl;

import com.mycollab.db.persistence.ICrudGenericDAO;
import com.mycollab.db.persistence.ISearchableDAO;
import com.mycollab.db.persistence.service.DefaultService;
import com.mycollab.module.crm.dao.ProductCatalogMapper;
import com.mycollab.module.crm.dao.ProductCatalogMapperExt;
import com.mycollab.module.crm.domain.ProductCatalog;
import com.mycollab.module.crm.domain.criteria.ProductCatalogSearchCriteria;
import com.mycollab.module.crm.service.ProductCatalogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductCatalogServiceImpl extends DefaultService<Integer, ProductCatalog, ProductCatalogSearchCriteria> implements ProductCatalogService {

    @Autowired
    private ProductCatalogMapper productCatalogMapper;

    @Autowired
    private ProductCatalogMapperExt productCatalogMapperExt;

    @Override
    public ICrudGenericDAO<Integer, ProductCatalog> getCrudMapper() {
        return productCatalogMapper;
    }

    @Override
    public ISearchableDAO<ProductCatalogSearchCriteria> getSearchMapper() {
        return productCatalogMapperExt;
    }

}
