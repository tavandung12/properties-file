package com.tvd12.properties.file.struct;

import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.tvd12.properties.file.bean.Transformer;
import com.tvd12.properties.file.constant.Constants;

/**
 * 
 * Holds structure of java bean class, map object to properties object
 * and also  
 * 
 * @author tavandung12
 *
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class PropertiesBean {

	private Object bean;
    private ClassWrapper wrapper;
    private volatile boolean inited = false;
    
	private static final Map<Class, Transformer> TRANSFORMERS =
            Collections.unmodifiableMap(createTypeTransformers());
	
	public PropertiesBean() {
	}

    public PropertiesBean(Class<?> clazz) {
        init(clazz);
    }
    
    public PropertiesBean(Object bean) {
        init(bean);
    }
    
    public void init(Class<?> clazz) {
    		if(!inited) {
    			this.inited = true;
    			this.wrapper = new ClassWrapper(clazz);
    			this.bean = wrapper.newInstance();
    		}
    }
    
    public void init(Object bean) {
		if(!inited) {
			this.inited = true;
			this.bean = bean;
			this.wrapper = new ClassWrapper(bean.getClass());
		}
    }
    
    public <T> T getObject() {
        return (T)bean;
    }

    protected Method getWriteMethod(Object key) {
        return getWriteMethod(key.toString());
    }

    public Method getWriteMethod(String key) {
        return wrapper.getMethod(key);
    }
    
    public void put(Object key, Object value) {
    		Method method = getWriteMethod(key);
        if(method == null)
            return;
        if(value instanceof String)
            value = ((String) value).trim();
        try {
        		Class argumentType = getWriteArgumentType(method);
        		Object argument = transform(argumentType, value);
        		method.invoke(bean, argument);
        }
        catch (Exception e) {
        		printError("put value: " + value + " with key: " + key + " error", e);
			return;
		}
    }
    
    public void putAll(Properties properties) {
        for(Object key : properties.keySet()) {
        		Object value = properties.get(key);
        		put(key, value);
        }
    }
    
    protected Class getWriteArgumentType(Method method) {
    		Class<?>[] parameterTypes = method.getParameterTypes();
    		Class answer = parameterTypes[0];
    		return answer;
    }
    
	protected Object transform(Class newType, Object value) {
		Transformer transformer = getTypeTransformer(newType);
		if (transformer == null)
			return value;
		Object answer = transformer.transform(value);
		return answer;
	}
    
    
    protected Transformer getTypeTransformer(Class aType) {
        Transformer transformer = TRANSFORMERS.get(aType);
        if(transformer == null)
            return null;
        return transformer;
    }
    
    protected void printError(String message, Throwable throwable) {
    		System.out.println(message + "\n" + throwable);
    }
    
    private static Map<Class, Transformer> createTypeTransformers() {
        Map<Class, Transformer> transformers = new HashMap<>();
        transformers.put(Boolean.TYPE, new Transformer() {
			public Object transform(Object input) {
				return Boolean.valueOf(input.toString());
			}
		});
		transformers.put(Character.TYPE, new Transformer() {
			public Object transform(Object input) {
				return new Character(input.toString().charAt(0));
			}
		});
		transformers.put(Byte.TYPE, new Transformer() {
			public Object transform(Object input) {
				return Byte.valueOf(input.toString());
			}
		});
		transformers.put(Double.TYPE, new Transformer() {
			public Object transform(Object input) {
				return Double.valueOf(input.toString());
			}
		});
		transformers.put(Float.TYPE, new Transformer() {
			public Object transform(Object input) {
				return Float.valueOf(input.toString());
			}
		});
		transformers.put(Integer.TYPE, new Transformer() {
			public Object transform(Object input) {
				return Integer.valueOf(input.toString());
			}
		});
		transformers.put(Long.TYPE, new Transformer() {
			public Object transform(Object input) {
				return Long.valueOf(input.toString());
			}
		});
		transformers.put(Short.TYPE, new Transformer() {
			public Object transform(Object input) {
				return Short.valueOf(input.toString());
			}
		});
		transformers.put(String.class, new Transformer() {
			public Object transform(Object input) {
				return input.toString();
			}
		});
		transformers.put(Date.class, new Transformer() {
            @Override
            public Object transform(Object value) {
            		String str = value.toString();
            		for(String pattern : Constants.DATE_FORMATS) {
            			SimpleDateFormat format = new SimpleDateFormat(pattern);
            			try {
            				return format.parse(str);
            			}
            			catch(Exception e) {
            				//ignore
            			}
            		}
            		throw new IllegalArgumentException("has no pattern to format date string: " + str);
            }
        });
        
        transformers.put(Class.class, new Transformer() {
            @Override
            public Object transform(Object value) {
                try {
                    String string = value.toString();
                    if(string.startsWith("class ")) 
                        string = string.substring("class ".length()).trim();
                    return Class.forName(string);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        });
        
		return transformers;
    }
    
}
