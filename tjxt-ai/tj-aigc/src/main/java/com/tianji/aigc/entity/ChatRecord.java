package com.tianji.aigc.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("chat_record")
@NoArgsConstructor
@AllArgsConstructor
public class ChatRecord {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String conversationId;

    /**
     * 对话数据：整个会话消息的 JSON 数组
     */
    private String data;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long creater;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updater;
}