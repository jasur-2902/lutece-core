/*
 * Copyright (c) 2002-2017, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.portal.web.workgroup;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Random;

import org.springframework.mock.web.MockHttpServletRequest;

import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.business.user.AdminUserHome;
import fr.paris.lutece.portal.business.workgroup.AdminWorkgroup;
import fr.paris.lutece.portal.business.workgroup.AdminWorkgroupHome;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.security.SecurityTokenService;
import fr.paris.lutece.test.LuteceTestCase;

public class AdminWorkgroupJspBeanTest extends LuteceTestCase
{
    private AdminWorkgroup adminWorkgroup;
    private AdminWorkgroupJspBean bean;

    @Override
    protected void setUp( ) throws Exception
    {
        super.setUp( );
        adminWorkgroup = new AdminWorkgroup( );
        adminWorkgroup.setKey( getRandomName( ) );
        adminWorkgroup.setDescription( adminWorkgroup.getKey( ) );
        AdminWorkgroupHome.create( adminWorkgroup );
        bean = new AdminWorkgroupJspBean( );
    }

    @Override
    protected void tearDown( ) throws Exception
    {
        AdminWorkgroupHome.removeAllUsersForWorkgroup( adminWorkgroup.getKey( ) );
        AdminWorkgroupHome.remove( adminWorkgroup.getKey( ) );
        super.tearDown( );
    }

    public void testDoAssignUsers( ) throws AccessDeniedException
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.setParameter( "workgroup_key", adminWorkgroup.getKey( ) );
        AdminUser user = AdminUserHome.findUserByLogin( "admin" );
        request.setParameter( "list_users", Integer.toString( user.getUserId( ) ) );
        request.setParameter( SecurityTokenService.PARAMETER_TOKEN, SecurityTokenService.getInstance( )
                .getToken( request, "admin/workgroup/assign_users_workgroup.html" ) );

        assertFalse( AdminWorkgroupHome.isUserInWorkgroup( user, adminWorkgroup.getKey( ) ) );
        bean.doAssignUsers( request );
        assertTrue( AdminWorkgroupHome.isUserInWorkgroup( user, adminWorkgroup.getKey( ) ) );
    }

    public void testDoAssignUsersInvalidToken( ) throws AccessDeniedException
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.setParameter( "workgroup_key", adminWorkgroup.getKey( ) );
        AdminUser user = AdminUserHome.findUserByLogin( "admin" );
        request.setParameter( "list_users", Integer.toString( user.getUserId( ) ) );
        request.setParameter( SecurityTokenService.PARAMETER_TOKEN,
                SecurityTokenService.getInstance( ).getToken( request, "admin/workgroup/assign_users_workgroup.html" )
                        + "b" );

        assertFalse( AdminWorkgroupHome.isUserInWorkgroup( user, adminWorkgroup.getKey( ) ) );
        try
        {
            bean.doAssignUsers( request );
            fail( "Should have thrown" );
        }
        catch ( AccessDeniedException e )
        {
            assertFalse( AdminWorkgroupHome.isUserInWorkgroup( user, adminWorkgroup.getKey( ) ) );
        }
    }

    public void testDoAssignUsersNoToken( ) throws AccessDeniedException
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.setParameter( "workgroup_key", adminWorkgroup.getKey( ) );
        AdminUser user = AdminUserHome.findUserByLogin( "admin" );
        request.setParameter( "list_users", Integer.toString( user.getUserId( ) ) );

        assertFalse( AdminWorkgroupHome.isUserInWorkgroup( user, adminWorkgroup.getKey( ) ) );
        try
        {
            bean.doAssignUsers( request );
            fail( "Should have thrown" );
        }
        catch ( AccessDeniedException e )
        {
            assertFalse( AdminWorkgroupHome.isUserInWorkgroup( user, adminWorkgroup.getKey( ) ) );
        }
    }

    private String getRandomName( )
    {
        Random rand = new SecureRandom( );
        BigInteger bigInt = new BigInteger( 128, rand );
        return "junit" + bigInt.toString( 36 );
    }
}