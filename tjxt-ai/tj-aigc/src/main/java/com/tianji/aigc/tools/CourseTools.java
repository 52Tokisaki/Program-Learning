package com.tianji.aigc.tools;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CourseBaseInfoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseTools {
    private final CourseClient courseClient;
    private static final String FIELD_NAME_FORMAT = "{}_{}";  // 提取格式字符串常量

    /**
     * 根据课程id查询课程信息
     *
     * @param courseId 课程id
     * @return 课程信息
     */
    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById (@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId, ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> courseClient.baseInfo(id, true))
                .map(CourseInfo::of)
                .map(courseInfo -> {
                    String requestId = MapUtil.get(toolContext.getContext(), Constant.REQUEST_ID, String.class); // 从上下文中去除传递的requestId
                    String field = StrUtil.format(FIELD_NAME_FORMAT, CourseInfo.class.getSimpleName(), courseInfo.getId());
                    ToolResultHolder.put(requestId, field, courseInfo); // 将课程信息放入工具结果持有器中传递给输出流
                    /*log.info("CourseTools put at {}, requestId={}, field={}", System.currentTimeMillis(), requestId, field);
                    log.info("CourseTools: map identity={}, classLoader={}, keys={}",
                            ToolResultHolder.mapIdentity(),
                            ToolResultHolder.holderClassLoader(),
                            ToolResultHolder.keys());*/
                    return courseInfo;
                })
                .orElse(null);
    }
}
