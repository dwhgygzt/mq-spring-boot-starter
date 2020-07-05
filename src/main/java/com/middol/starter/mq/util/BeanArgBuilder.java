package com.middol.starter.mq.util;

/**
 * 动态注入bean的一些参数
 *
 * @author <a href="mailto:guzhongtao@middol.com">guzhongtao</a>
 */
public class BeanArgBuilder {

    private Object[] constructorArgs;

    private String initMethodName;

    private String destoryMethodName;

    public BeanArgBuilder(Object[] constructorArgs, String initMethodName, String destoryMethodName) {
        this.constructorArgs = constructorArgs;
        this.initMethodName = initMethodName;
        this.destoryMethodName = destoryMethodName;
    }

    public BeanArgBuilder() {
    }

    public Object[] getConstructorArgs() {
        return constructorArgs;
    }

    public void setConstructorArgs(Object[] constructorArgs) {
        this.constructorArgs = constructorArgs;
    }

    public String getInitMethodName() {
        return initMethodName;
    }

    public void setInitMethodName(String initMethodName) {
        this.initMethodName = initMethodName;
    }

    public String getDestoryMethodName() {
        return destoryMethodName;
    }

    public void setDestoryMethodName(String destoryMethodName) {
        this.destoryMethodName = destoryMethodName;
    }
}
