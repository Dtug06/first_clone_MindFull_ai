import { useState } from 'react';
import { motion } from 'framer-motion';
import RiskLevelBadge from '../../components/ui/RiskLevelBadge';
import { Search, MoreVertical } from 'lucide-react';
import { RiskLevel } from '../../types';

const users = [
  { id: '1', name: 'Nguyen Van A', email: 'nva@email.com', status: 'active', lastCheckIn: '2 hours ago', riskLevel: 1 as RiskLevel },
  { id: '2', name: 'Tran Thi B', email: 'ttb@email.com', status: 'active', lastCheckIn: '1 day ago', riskLevel: 2 as RiskLevel },
  { id: '3', name: 'Le Van C', email: 'lvc@email.com', status: 'inactive', lastCheckIn: '5 days ago', riskLevel: 1 as RiskLevel },
  { id: '4', name: 'Pham Thi D', email: 'ptd@email.com', status: 'active', lastCheckIn: '3 hours ago', riskLevel: 3 as RiskLevel },
  { id: '5', name: 'Hoang Van E', email: 'hve@email.com', status: 'active', lastCheckIn: '6 hours ago', riskLevel: 1 as RiskLevel },
];

export default function UserManagement() {
  const [searchQuery, setSearchQuery] = useState('');
  const [filterStatus, setFilterStatus] = useState('all');

  const filteredUsers = users.filter(user => {
    const matchesSearch = user.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         user.email.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesStatus = filterStatus === 'all' || user.status === filterStatus;
    return matchesSearch && matchesStatus;
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
      >
        <h1 className="text-2xl font-semibold text-textMain">User Management</h1>
        <p className="text-textMuted">Manage and monitor user accounts and wellness status.</p>
      </motion.div>

      {/* Filters */}
      <motion.div
        className="bg-surface rounded-2xl p-4 shadow-soft"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
      >
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-textMuted" />
            <input
              type="text"
              placeholder="Search users..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2 bg-surfaceMuted rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>
          <div className="flex gap-2">
            {['all', 'active', 'inactive'].map((status) => (
              <button
                key={status}
                onClick={() => setFilterStatus(status)}
                className={`px-4 py-2 rounded-xl text-sm font-medium capitalize transition-colors ${
                  filterStatus === status
                    ? 'bg-primary text-white'
                    : 'bg-surfaceMuted text-textMuted hover:bg-gray-200'
                }`}
              >
                {status}
              </button>
            ))}
          </div>
        </div>
      </motion.div>

      {/* Users table */}
      <motion.div
        className="bg-surface rounded-2xl shadow-soft overflow-hidden"
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
      >
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-surfaceMuted">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">User</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">Status</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">Last Check-in</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">Risk Level</th>
                <th className="px-6 py-3 text-left text-xs font-medium text-textMuted uppercase tracking-wider">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {filteredUsers.map((user) => (
                <tr key={user.id} className="hover:bg-surfaceMuted/50 transition-colors">
                  <td className="px-6 py-4">
                    <div>
                      <div className="font-medium text-textMain">{user.name}</div>
                      <div className="text-sm text-textMuted">{user.email}</div>
                    </div>
                  </td>
                  <td className="px-6 py-4">
                    <span className={`inline-flex px-2 py-1 text-xs font-medium rounded-full ${
                      user.status === 'active' 
                        ? 'bg-primary/10 text-primary' 
                        : 'bg-gray-100 text-textMuted'
                    }`}>
                      {user.status}
                    </span>
                  </td>
                  <td className="px-6 py-4 text-sm text-textMuted">{user.lastCheckIn}</td>
                  <td className="px-6 py-4">
                    <RiskLevelBadge level={user.riskLevel} size="sm" />
                  </td>
                  <td className="px-6 py-4">
                    <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                      <MoreVertical className="w-4 h-4 text-textMuted" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </motion.div>
    </div>
  );
}
