package com.hxzhou.mall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.hxzhou.common.constant.ProductConstant;
import com.hxzhou.mall.product.dao.AttrAttrgroupRelationDao;
import com.hxzhou.mall.product.dao.AttrGroupDao;
import com.hxzhou.mall.product.dao.CategoryDao;
import com.hxzhou.mall.product.entity.AttrAttrgroupRelationEntity;
import com.hxzhou.mall.product.entity.AttrGroupEntity;
import com.hxzhou.mall.product.entity.CategoryEntity;
import com.hxzhou.mall.product.service.CategoryService;
import com.hxzhou.mall.product.vo.AttrGroupRelationVo;
import com.hxzhou.mall.product.vo.AttrRespVo;
import com.hxzhou.mall.product.vo.AttrVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hxzhou.common.utils.PageUtils;
import com.hxzhou.common.utils.Query;

import com.hxzhou.mall.product.dao.AttrDao;
import com.hxzhou.mall.product.entity.AttrEntity;
import com.hxzhou.mall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Autowired
    AttrAttrgroupRelationDao relationDao;

    @Transactional
    @Override
    public void saveAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);

        // 保存基本数据
        this.save(attrEntity);

        // 保存关联关系
        if(attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() && attr.getAttrGroupId() != null) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attrEntity.getAttrId());
            relationDao.insert(relationEntity);
        }

    }

    @Autowired
    AttrGroupDao attrGroupDao;

    @Autowired
    CategoryDao categoryDao;

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>()
                .eq("attr_type", "base".equalsIgnoreCase(type) ?
                        ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());

        if(catelogId != 0) queryWrapper.eq("catelog_id", catelogId);

        String key = (String) params.get("key");
        if(!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );

        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> respVos = records.stream().map((attrEntity) -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);

            // 设置分类名字
            if("base".equalsIgnoreCase(type)) {
                AttrAttrgroupRelationEntity attrId = relationDao.selectOne(
                        new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
                if (attrId != null && attrId.getAttrGroupId() != null) {
                    AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrId.getAttrGroupId());
                    attrRespVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }


            // 设置分类名字
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) attrRespVo.setCatelogName(categoryEntity.getName());

            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(respVos);
        return pageUtils;
    }

    @Autowired
    CategoryService categoryService;

    @Override
    public AttrRespVo getAttrInfo(Long attrId) {
        AttrRespVo respVo = new AttrRespVo();

        // 当前属性的详细信息
        AttrEntity attrEntity = this.getById(attrId);
        BeanUtils.copyProperties(attrEntity, respVo);

        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 查询当前属性的分组信息
            AttrAttrgroupRelationEntity attrgroupRelation = relationDao.selectOne(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if(attrgroupRelation != null) {
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupRelation.getAttrGroupId());
                respVo.setAttrGroupId(attrgroupRelation.getAttrGroupId());

                if(attrGroupEntity != null) {
                    respVo.setGroupName(attrGroupEntity.getAttrGroupName());
                }
            }
        }


        // 查询当前属性的分类信息
        Long catelogId = attrEntity.getCatelogId();
        Long[] catelogPath = categoryService.findCatelogPath(catelogId);
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        respVo.setCatelogPath(catelogPath);
        if(categoryEntity != null) respVo.setCatelogName(categoryEntity.getName());

        return respVo;
    }

    @Transactional
    @Override
    public void updateAttr(AttrVo attr) {
        AttrEntity attrEntity = new AttrEntity();

        // 更新基础信息
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);

        if(attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            // 修改分组关联
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            relationEntity.setAttrId(attr.getAttrId());

            Integer count = relationDao.selectCount(
                    new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));

            // 如果以前有值，执行更新操作
            if(count > 0) {
                relationDao.update(relationEntity,
                        new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            }
            // 如果以前没有值，执行增加操作
            else relationDao.insert(relationEntity);
        }

    }

    /**
     * 根据分组id查找关联的所有基本属性（规格参数）
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {
        List<AttrAttrgroupRelationEntity> entities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>()
                .eq("attr_group_id", attrgroupId));

        List<Long> attrIds = entities.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());

        if(attrIds == null || attrIds.size() == 0) return null;

        Collection<AttrEntity> attrEntities = this.listByIds(attrIds);
        return (List<AttrEntity>) attrEntities;
    }

    /**
     * 删除关联关系
     * @param vos
     */
    @Override
    public void deleteRelation(AttrGroupRelationVo[] vos) {
        // 自定义批量删除
        List<AttrAttrgroupRelationEntity> entities = Arrays.asList(vos).stream().map((item) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(item, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());

        relationDao.deleteBatchRelation(entities);
    }

    /**
     * 查询没有关联的信息
     * @param params
     * @param attrgroupId
     * @return
     */
    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {
        /**
         * 1 当前分组只能关联自己所属的分类里面的所有属性
         */
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        Long catelogId = attrGroupEntity.getCatelogId();

        /**
         * 2 当前分组只能关联别的分组没有引用的属性
         */
        // 2.1 当前分类下的其他分组
        List<AttrGroupEntity> group = attrGroupDao.selectList(
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<Long> collect = group.stream().map((item) -> {
            return item.getAttrGroupId();
        }).collect(Collectors.toList());

        // 2.2 这些分组关联的属性
        List<AttrAttrgroupRelationEntity> groupId = relationDao.selectList(
                new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", collect));
        List<Long> attrIds = groupId.stream().map((item) -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        // 2.3 从当前分类的所有属性中移除这些属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId)
                .eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if(attrIds != null && attrIds.size() > 0) {
            wrapper.notIn("attr_id", attrIds);
        }

        String key = (String) params.get("key");
        if(StringUtils.hasLength(key)) {
            wrapper.and((w) -> {
                w.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        return new PageUtils(page);
    }

}