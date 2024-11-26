import React, { useState, useRef, useEffect } from 'react';
import { useKeycloak } from '../provider/KeycloakProvider';

const LogoutButton = () => {
  const { logout, user } = useKeycloak();
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef(null);

  const toggleDropdown = () => {
    setShowDropdown(!showDropdown);
  };

  useEffect(() => {
    const handleClickOutside = event => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setShowDropdown(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  return (
    <div className="relative inline-block text-left">
      <button onClick={toggleDropdown} className="button">
        {user?.name.charAt(0).toUpperCase() || 'U'}
      </button>
      {showDropdown && (
        <div ref={dropdownRef} className="dropdown">
          <div className="dropdown-content">
            <p>{user?.name || 'Unknown User'}</p>
            <p>{user?.email || 'Unknown User'}</p>
            <button onClick={logout}>Logout</button>
          </div>
        </div>
      )}
    </div>
  );
};

export default LogoutButton;