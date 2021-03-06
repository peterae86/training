package com.demo.mybatis.accessor;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.ibatis.session.RowBounds;
import org.springframework.transaction.annotation.Transactional;

import com.demo.mybatis.accessor.bean.ListQuery;
import com.demo.mybatis.accessor.bean.ListResult;
import com.demo.mybatis.accessor.bean.Query;

public class GenericModelAccessor<T> extends AbstractModelAccessor implements ModelAccessor<T>{

    // SQL_ID列表
    protected final String SQL_SELECT;
    protected final String SQL_SELECT_COUNT;
    protected final String SQL_INSERT;
    protected final String SQL_UDPATE;
    protected final String SQL_DELETE;

    // 配置
    private int startIndex = 0;
    private int maxCount = 1024;

    public GenericModelAccessor(String model) {
        SQL_SELECT = model + ".select";
        SQL_SELECT_COUNT = model + ".selectCount";
        SQL_INSERT = model + ".insert";
        SQL_UDPATE = model + ".update";
        SQL_DELETE = model + ".delete";

        // Load configuration
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(GenericModelAccessor.class.getName());
            if (bundle.containsKey("startIndex")) {
                this.startIndex = Integer.valueOf(bundle.getString("startIndex"));
            }
            if (bundle.containsKey("maxCount")) {
                this.maxCount = Integer.valueOf(bundle.getString("maxCount"));
            }
        } catch (MissingResourceException e) {
            // No Resource
        }

        logger.debug("default start index: {}", startIndex);
        logger.debug("default max count: {}", maxCount);
    }

    @Override
    public List<T> select(ListQuery query) {

        return select(SQL_SELECT, query);
    }

    protected List<T> select(String sqlId, ListQuery query) {

        Integer startIndex = query.getStartIndex();
        Integer maxCount = query.getMaxCount();

        if (startIndex == null && maxCount == null) {
            return sqlSession.selectList(sqlId, query);
        }

        startIndex = (startIndex == null || startIndex < this.startIndex) ? this.startIndex : startIndex.intValue();
        maxCount = (maxCount == null || maxCount > this.maxCount || maxCount <= 0) ? this.maxCount : maxCount.intValue();

        return sqlSession.selectList(sqlId, query, new RowBounds(startIndex, maxCount));
    }

    @Override
    public int selectCount(Query query) {
        return sqlSession.selectOne(SQL_SELECT_COUNT, query);
    }

    @Override
    public ListResult<T> list(ListQuery query) {

        int totalCount = selectCount(query);
        if (totalCount == 0) {
            return new ListResult<T>(new ArrayList<T>(0));
        }

        List<T> list = select(query); // queryDAO.executeForObjectList(SQL_SELECT, query, query.getStartIndex(), query.getMaxCount());

        return new ListResult<T>(list, totalCount);
    }

    @Override
    public int insert(T object) {

        if(object == null) {
            return 0;
        }

        return sqlSession.update(SQL_INSERT, object);
    }

    @Override
    @Transactional
    public int insert(T[] list) {

        if (list == null || list.length == 0) {
            return 0;
        }

        // batchInsert or loop (when database not supported) 
        int ret = 0;
        for (T obj: list) {
            ret = ret + insert(obj);
        }

        return ret;
    }

    @Override
    public int update(T object) {
        return sqlSession.update(SQL_UDPATE, object);
    }

    @Override
    public int delete(int id) {
        return sqlSession.update(SQL_DELETE, id);
    }

}
