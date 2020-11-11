package io.nessus.common.rest;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;

import io.nessus.common.Config;
import io.nessus.common.ConfigSupport;
import io.undertow.security.idm.Account;
import io.undertow.security.idm.Credential;
import io.undertow.security.idm.IdentityManager;

public abstract class AbstractIdentityManager<T extends Config> extends ConfigSupport<T> implements IdentityManager {

	public AbstractIdentityManager(T config) {
		super(config);
	}

	@Override
    public Account verify(Account account) {
        return account;
    }

    @Override
    public Account verify(String id, Credential credential) {
    	Account account = new BasicAccount(id);
    	boolean access = verify(account, credential);
        return access ? account : null;
    }

    public abstract boolean verify(Account account, Credential credential);

	@Override
    public Account verify(Credential credential) {
        return null;
    }

    public static class BasicPricipal implements Principal {

    	private final String id;
    	
        public BasicPricipal(String id) {
			this.id = id;
		}

		public String getName() {
            return id;
        }
    }
    
    @SuppressWarnings("serial")
	public static class BasicAccount implements Account {

    	private final String id;
    	
        public BasicAccount(String id) {
			this.id = id;
		}
        
        @Override
        public Principal getPrincipal() {
            return new BasicPricipal(id);
        }

        @Override
        public Set<String> getRoles() {
            return Collections.emptySet();
        }
        
        @Override
        public String toString() {
        	return String.format("BasicAccount[id=%s, roles=%s]", id, getRoles());
        }
    }
}